package nu.marginalia.control.node.svc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import nu.marginalia.client.Context;
import nu.marginalia.control.Redirects;
import nu.marginalia.executor.client.ExecutorClient;
import nu.marginalia.storage.FileStorageService;
import nu.marginalia.storage.model.FileStorageId;
import nu.marginalia.storage.model.FileStorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

@Singleton
public class ControlFileStorageService {
    private final FileStorageService fileStorageService;
    private final ExecutorClient executorClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public ControlFileStorageService(FileStorageService fileStorageService, ExecutorClient executorClient)
    {
        this.fileStorageService = fileStorageService;
        this.executorClient = executorClient;
    }

    public void register() throws IOException {
        Spark.post("/public/storage/:fid/delete", this::flagFileForDeletionRequest, Redirects.redirectToStorage);

        Spark.post("/public/nodes/:id/storage/:fid/delete", this::deleteFileStorage);
        Spark.post("/public/nodes/:id/storage/:fid/enable", this::enableFileStorage);
        Spark.post("/public/nodes/:id/storage/:fid/disable", this::disableFileStorage);
        Spark.get("/public/nodes/:id/storage/:fid/transfer", this::downloadFileFromStorage);

    }

    @SneakyThrows
    public String redirectToOverview(int nodeId) {
        return new Redirects.HtmlRedirect("/nodes/"+nodeId).render(null);
    }

    @SneakyThrows
    public String redirectToOverview(Request request) {
        return redirectToOverview(Integer.parseInt(request.params("id")));
    }

    private Object deleteFileStorage(Request request, Response response) throws SQLException {
        int nodeId = Integer.parseInt(request.params("id"));
        int fileId = Integer.parseInt(request.params("fid"));

        fileStorageService.flagFileForDeletion(new FileStorageId(fileId));

        return redirectToOverview(request);
    }

    public Object downloadFileFromStorage(Request request, Response response) throws IOException {
        int nodeId = Integer.parseInt(request.params("id"));
        var fileStorageId = FileStorageId.parse(request.params("fid"));

        String path = request.queryParams("path");

        response.header("content-disposition", "attachment; filename=\""+path+"\"");

        if (path.endsWith(".txt") || path.endsWith(".log"))
            response.type("text/plain");
        else
            response.type("application/octet-stream");

        executorClient.transferFile(Context.fromRequest(request), nodeId, fileStorageId, path, response.raw().getOutputStream());

        return "";
    }

    private Object enableFileStorage(Request request, Response response) throws SQLException {
        int nodeId = Integer.parseInt(request.params("id"));
        FileStorageId fileId = new FileStorageId(Integer.parseInt(request.params("fid")));

        var storage = fileStorageService.getStorage(fileId);
        if (storage.type() == FileStorageType.CRAWL_DATA) {
            fileStorageService.disableFileStorageOfType(nodeId, storage.type());
        }

        fileStorageService.enableFileStorage(fileId);

        return "";
    }

    private Object disableFileStorage(Request request, Response response) throws SQLException {
        int nodeId = Integer.parseInt(request.params("id"));
        int fileId = Integer.parseInt(request.params("fid"));

        fileStorageService.disableFileStorage(new FileStorageId(fileId));

        return "";
    }
    public Object flagFileForDeletionRequest(Request request, Response response) throws SQLException {
        FileStorageId fid = new FileStorageId(Long.parseLong(request.params(":fid")));
        fileStorageService.flagFileForDeletion(fid);
        return "";
    }


}
