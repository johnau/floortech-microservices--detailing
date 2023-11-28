package tech.jmcs.floortech.detailing.infrastructure.files.storage;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.common.helper.FileHelper;
import tech.jmcs.floortech.detailing.infrastructure.files.storage.exception.FileStorageException;
import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    public static final String DELETED_FOLDER_PREFIX = "_DELETED_";
    public static final String TEMP_FOLDER_PREFIX = "_TEMP_";
    final Environment env;
    private String fileStorageRoot;
    private Path root;
    private Path deletedFilesPath;
    private Path temporaryFilesPath;

    @Autowired
    public FileStorageServiceImpl(Environment env) {
        this.env = env;
    }

    /**
     * Setup root path variables by OS
     */
    @PostConstruct
    private void init() {
        System.out.println("Init running");
        var linuxRoot = env.getProperty("files.storage-root-linux");
        var windowsRoot = env.getProperty("files.storage-root");
        if (SystemUtils.IS_OS_LINUX) {
            this.fileStorageRoot = linuxRoot;
        } else {
            this.fileStorageRoot = windowsRoot;
        }
        this.root = Paths.get(fileStorageRoot);
        this.deletedFilesPath =  Paths.get(fileStorageRoot, DELETED_FOLDER_PREFIX);
        this.temporaryFilesPath = Paths.get(fileStorageRoot, TEMP_FOLDER_PREFIX);

        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public Path makeRelative(Path path) {
        var isTemp = path.toString().startsWith(temporaryFilesPath.toString());
        var isDeleted = path.toString().startsWith(deletedFilesPath.toString());
        var isRoot = path.toString().startsWith(root.toString()) && !isTemp && !isDeleted;

        if (isRoot) {
            return Paths.get(path.toString().substring(root.toString().length()));
        } else if (isTemp) {
            return Paths.get(path.toString().substring(temporaryFilesPath.toString().length()));
        } else if (isTemp) {
            return Paths.get(path.toString().substring(deletedFilesPath.toString().length()));
        } else {
            log.info("Assuming path is already relative");
            return path;
        }
    }

    @Override
    public Path makeAbsolute(Path relativePath) {
        if (relativePath.toString().startsWith(root.toString())) {
            log.info("Already absolute: {}", relativePath);
            return relativePath;
        }
        var absolutePath = Paths.get(root.toString(), relativePath.toString());
        var exists = Files.exists(absolutePath, LinkOption.NOFOLLOW_LINKS);
        return exists ? absolutePath : null;
    }

    /**
     * Saves a file to root storage inside folder path created by pathParts, (ie (filePart, "a", "b"), will be
     * stored in root/a/b/filename.ext
     * The absolute path is returned
     * @param filePartMono
     * @param pathParts
     * @return
     */
    @Override
    public Mono<String> saveToTemp(Mono<FilePart> filePartMono, String... pathParts) {
        return filePartMono
                .flatMap(filePart -> writeFileMono(temporaryFilesPath, filePart, pathParts))
                .map(Path::toString);
    }

    @Override
    public Mono<Path> saveToRoot(FilePart filePart, String... pathParts) {
        return writeFileMono(root, filePart, pathParts);
    }

    @Override
    public Flux<DataBuffer> loadFromRoot(XPath xPath) {
        if (xPath.isRelative()) {
            return loadFile(Paths.get(root.toString(), xPath.path()));
        } else {
            return loadFile(xPath.toPath());
        }
    }

    @Override
    public Flux<DataBuffer> loadFromRoot(String relativePath) {
        return loadFromRoot(Paths.get(relativePath));
    }

    @Override
    public Flux<DataBuffer> loadFromRoot(Path relativePath) {
        return loadFile(Paths.get(root.toString(), relativePath.toString()));
    }

    @Override
    public Path softDelete(String relativePath) {
        try {
            var sourcePath = Paths.get(root.toString(), relativePath);
            sourcePath = root.resolve(sourcePath);

            var rel = Paths.get(relativePath);
            var filename = rel.getFileName().toString();
            var folders = rel.getParent().toString();
            var uniqueFilename = FileHelper.generateUniqueFilename(deletedFilesPath, folders, filename);

            var destPath = Paths.get(deletedFilesPath.toString(), folders, uniqueFilename);
            Files.createDirectories(destPath.getParent());
            Files.move(sourcePath, destPath);
            return Paths.get(DELETED_FOLDER_PREFIX, folders, uniqueFilename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the absolute path of the written file
     * @param pathRoot
     * @param filePart
     * @param pathParts
     * @return
     */
    private Mono<Path> writeFileMono(Path pathRoot, FilePart filePart, String... pathParts) {
        var subFolderPath = FileHelper.buildPath(pathParts);
        var filename = FileHelper.generateUniqueFilename(pathRoot, subFolderPath.toString(), filePart.filename());
        Path absoluteParentFolderPath = null;
        try {
            absoluteParentFolderPath = FileHelper.createDirectoriesAndFile(pathRoot, subFolderPath, filename);
        } catch (IOException e) {
            return Mono.error(new FileStorageException("File Storage Error: unable to write file: " + filename));
        }
        var path = absoluteParentFolderPath.resolve(filename);
        log.info("Destination path={}",path);
        return filePart
                .transferTo(path)
                .thenReturn(path);
    }

    private Flux<DataBuffer> loadFile(Path filePath) {
        try {
            var resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 4096);
            } else {
                throw new RuntimeException("Could not read the file! " + filePath.toString());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

}
