package nu.marginalia.index.forward;

import com.upserve.uppend.blobs.NativeIO;
import gnu.trove.map.hash.TLongIntHashMap;
import nu.marginalia.array.LongArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static nu.marginalia.index.forward.ForwardIndexParameters.*;

public class ForwardIndexReader {
    private final TLongIntHashMap ids;
    private final LongArray data;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ForwardIndexReader(Path idsFile, Path dataFile) throws IOException {
        if (!Files.exists(dataFile)) {
            logger.warn("Failed to create ForwardIndexReader, {} is absent", dataFile);
            ids = null;
            data = null;
            return;
        }
        else if (!Files.exists(idsFile)) {
            logger.warn("Failed to create ForwardIndexReader, {} is absent", idsFile);
            ids = null;
            data = null;
            return;
        }

        logger.info("Switching forward index");

        ids = loadIds(idsFile);
        data = loadData(dataFile);
    }

    private static TLongIntHashMap loadIds(Path idsFile) throws IOException {
        var idsArray = LongArray.mmapRead(idsFile);
        idsArray.advice(NativeIO.Advice.Sequential);

        var ids = new TLongIntHashMap((int) idsArray.size(), 0.5f, -1, -1);

        // This hash table should be of the same size as the number of documents, so typically less than 1 Gb
        idsArray.forEach(0, idsArray.size(), (pos, val) -> {
            ids.put(val, (int) pos);
        });

        return ids;
    }

    private static LongArray loadData(Path dataFile) throws IOException {
        var data = LongArray.mmapRead(dataFile);

        data.advice(NativeIO.Advice.Random);

        return data;
    }

    public long getDocMeta(long docId) {
        long offset = idxForDoc(docId);
        if (offset < 0) return 0;

        return data.get(ENTRY_SIZE * offset + METADATA_OFFSET);
    }

    public int getDomainId(long docId) {
        long offset = idxForDoc(docId);
        if (offset < 0) return 0;

        return Math.max(0, (int) data.get(ENTRY_SIZE * offset + DOMAIN_OFFSET));
    }

    public DocPost docPost(long docId) {
        long offset = idxForDoc(docId);
        if (offset < 0) throw new IllegalStateException("Forward index is not loaded");

        return new DocPost(offset);
    }

    private int idxForDoc(long docId) {
        return ids.get(docId);
    }


    public class DocPost {
        private final long idx;

        public DocPost(long idx) {
            this.idx = idx;
        }

        public long meta() {

            if (idx < 0)
                return 0;

            return data.get(ENTRY_SIZE * idx + METADATA_OFFSET);
        }

        public int domainId() {
            if (idx < 0)
                return 0;

            return Math.max(0, (int) data.get(ENTRY_SIZE * idx + DOMAIN_OFFSET));
        }
    }
}
