package nu.marginalia.array.page;

import com.upserve.uppend.blobs.NativeIO;
import nu.marginalia.array.ArrayRangeReference;
import nu.marginalia.array.LongArray;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LongArrayPage implements PartitionPage, LongArray {

    final LongBuffer longBuffer;
    @Nullable
    private final Arena arena;
    final ByteBuffer byteBuffer;
    private boolean closed;

    LongArrayPage(ByteBuffer byteBuffer,
                  @Nullable Arena arena) {
        this.byteBuffer = byteBuffer;
        this.longBuffer = byteBuffer.asLongBuffer();
        this.arena = arena;
    }

    public static LongArrayPage onHeap(int size) {
        var arena = Arena.ofShared();
        return new LongArrayPage(arena.allocate((long) WORD_SIZE*size, 8).asByteBuffer(), arena);
    }

    public static LongArrayPage fromMmapReadOnly(Path file, long offset, int size) throws IOException {
        var arena = Arena.ofShared();

        return new LongArrayPage(
                mmapFile(arena, file, offset, size, FileChannel.MapMode.READ_ONLY, StandardOpenOption.READ),
                arena);
    }

    public static LongArrayPage fromMmapReadWrite(Path file, long offset, int size) throws IOException {
        var arena = Arena.ofShared();

        return new LongArrayPage(
                mmapFile(arena, file, offset, size, FileChannel.MapMode.READ_WRITE,
                        StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE),
                arena);
    }

    private static ByteBuffer mmapFile(Arena arena,
                                       Path file,
                                       long offset,
                                       int size,
                                       FileChannel.MapMode mode,
                                       OpenOption... openOptions) throws IOException
    {
        try (var channel = (FileChannel) Files.newByteChannel(file, openOptions)) {

            return channel.map(mode,
                            WORD_SIZE*offset,
                            (long) size*WORD_SIZE,
                            arena)
                    .asByteBuffer();
        }
        catch (IOException ex) {
            throw new IOException("Failed to map file " + file + " (" + offset + ":" + size + ")", ex);
        }
    }

    @Override
    public long get(long at) {
        try {
            return longBuffer.get((int) at);
        }
        catch (IndexOutOfBoundsException ex) {
            throw new IndexOutOfBoundsException("@" + at + "(" + 0 + ":" + longBuffer.capacity() + ")");
        }
    }

    @Override
    public void get(long start, long end, long[] buffer) {
        longBuffer.get((int) start, buffer, 0, (int) (end - start));
    }

    @Override
    public void set(long at, long val) {
        longBuffer.put((int) at, val);
    }

    @Override
    public void set(long start, long end, LongBuffer buffer, int bufferStart) {
        longBuffer.put((int) start, buffer, bufferStart, (int) (end-start));
    }

    @Override
    public synchronized void close() {
        if (arena != null) {
            if (!closed) {
                arena.close();
                closed = true;
            }
        }
    }

    @Override
    public long size() {
        return longBuffer.capacity();
    }

    public void increment(int at) {
        set(at, get(at) + 1);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public void write(Path filename) throws IOException {
        try (var channel = (FileChannel) Files.newByteChannel(filename, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            write(channel);
        }
    }

    @Override
    public void force() {
        if (byteBuffer instanceof MappedByteBuffer mb) {
            mb.force();
        }
    }
    public ArrayRangeReference<LongArray> directRangeIfPossible(long start, long end) {
        return new ArrayRangeReference<>(this, start, end);
    }

    @Override
    public void transferFrom(FileChannel source, long sourceStart, long arrayStart, long arrayEnd) throws IOException {

        int index = (int) (arrayStart * WORD_SIZE);
        int length = (int) ((arrayEnd - arrayStart) * WORD_SIZE);

        var slice = byteBuffer.slice(index, length);

        long startPos = sourceStart * WORD_SIZE;
        while (slice.position() < slice.capacity()) {
            source.read(slice, startPos + slice.position());
        }
    }

    @Override
    public void advice(NativeIO.Advice advice) throws IOException {
        NativeIO.madvise((MappedByteBuffer) byteBuffer, advice);
    }

    @Override
    public void advice(NativeIO.Advice advice, long start, long end) throws IOException {
        NativeIO.madviseRange((MappedByteBuffer) byteBuffer, advice, (int) start, (int) (end-start));
    }

}
