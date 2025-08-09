package PartB.SemanticSearch.service;


import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@Service
public class RedisVectorService implements InitializingBean {

    private static final String INDEX = "idx:houses";
    private static final String PREFIX = "house:";

    // text-embedding-3-small is 1536-dim (float32)
    private static final int DIM = 1536;

    private final StringRedisTemplate redis;
    private final EmbeddingClient embeddingClient;

    public RedisVectorService(StringRedisTemplate redis, EmbeddingClient embeddingClient) {
        this.redis = redis;
        this.embeddingClient = embeddingClient;
    }

    /** Create the RediSearch vector index if it doesn't exist. Runs at startup. */
    @Override
    public void afterPropertiesSet() {
        redis.execute((RedisConnection connection) -> {
            RedisCommands<String, String> cmd = (RedisCommands<String, String>) connection.getNativeConnection();
            try {
                // Try to read info — if it fails, we create the index
                cmd.ftInfo(INDEX);
            } catch (Exception notExists) {
                // FT.CREATE idx:houses ON HASH PREFIX 1 house:
                // SCHEMA title TEXT description TEXT location TEXT price NUMERIC
                // vec VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE
                List<String> args = new ArrayList<>(List.of(
                        "FT.CREATE", INDEX, "ON", "HASH", "PREFIX", "1", PREFIX,
                        "SCHEMA",
                        "title", "TEXT",
                        "description", "TEXT",
                        "location", "TEXT",
                        "price", "NUMERIC",
                        "vec", "VECTOR", "HNSW", "6",
                        "TYPE", "FLOAT32",
                        "DIM", String.valueOf(DIM),
                        "DISTANCE_METRIC", "COSINE"
                ));
                cmd.dispatch(io.lettuce.core.protocol.CommandType.valueOf("FT.CREATE"),
                        new io.lettuce.core.output.StatusOutput<>(io.lettuce.core.codec.StringCodec.UTF8),
                        args.toArray(new String[0]));
            }
            return null;
        });
    }

    /** Add house + embedding to Redis. Returns assigned id. */
    public String addHouse(RentalHouse h) {
        String id = UUID.randomUUID().toString();
        h.setId(id);

        // 1) Compute embedding from title + description + location
        String text = (h.getTitle() + ". " + h.getDescription() + ". Location: " + h.getLocation()).trim();
        EmbeddingResponse er = embeddingClient.embed(text);
        List<Double> vec = er.getOutput().get(0).getEmbedding();

        // 2) Convert List<Double> to FLOAT32 (little endian) BLOB
        byte[] blob = toFloat32Blob(vec);

        // 3) Store Hash + vector
        String key = PREFIX + id;
        Map<String, String> map = new HashMap<>();
        map.put("title", nullToEmpty(h.getTitle()));
        map.put("description", nullToEmpty(h.getDescription()));
        map.put("location", nullToEmpty(h.getLocation()));
        map.put("price", String.valueOf(h.getPrice()));
        // store fields first
        redis.opsForHash().putAll(key, (Map) map);
        // store vector blob (raw) via low-level command HSET key vec <blob>
        redis.execute((RedisConnection connection) -> {
            connection.hashCommands().hSet(key.getBytes(), "vec".getBytes(), blob);
            return null;
        });

        return id;
    }

    /** Semantic KNN search: returns list of keys sorted by similarity (closest first). */
    public List<Map<String, Object>> search(String prompt, int k) {
        EmbeddingResponse er = embeddingClient.embed(prompt);
        List<Double> vec = er.getOutput().get(0).getEmbedding();
        byte[] blob = toFloat32Blob(vec);

        return redis.execute((RedisConnection connection) -> {
            RedisCommands<byte[], byte[]> cmd = (RedisCommands<byte[], byte[]>) connection.getNativeConnection();

            // RediSearch syntax (dialect 2):
            // FT.SEARCH idx:houses "*=>[KNN k @vec $BLOB]" PARAMS 2 BLOB <blob> SORTBY __score
            // RETURN 4 title description location price LIMIT 0 k DIALECT 2
            List<byte[]> args = new ArrayList<>();
            args.addAll(List.of(
                    "FT.SEARCH".getBytes(),
                    INDEX.getBytes(),
                    "*=>[KNN".getBytes(),
                    String.valueOf(k).getBytes(),
                    "@vec".getBytes(),
                    "$BLOB".getBytes(),
                    "]".getBytes()
            ));
            args.addAll(List.of(
                    "PARAMS".getBytes(), "2".getBytes(),
                    "BLOB".getBytes(), blob,
                    "SORTBY".getBytes(), "__score".getBytes(),
                    "RETURN".getBytes(), "8".getBytes(),
                    "title".getBytes(), "description".getBytes(), "location".getBytes(), "price".getBytes(),
                    "WITHSCORES".getBytes(),
                    "LIMIT".getBytes(), "0".getBytes(), String.valueOf(k).getBytes(),
                    "DIALECT".getBytes(), "2".getBytes()
            ));

            var output = new io.lettuce.core.output.MultiTypeOutput<>(io.lettuce.core.codec.ByteArrayCodec.INSTANCE);
            cmd.dispatch(io.lettuce.core.protocol.CommandType.valueOf("FT.SEARCH"), output, args.toArray(new byte[0][]));

            // Parse the RediSearch response into simple maps
            return RedisSearchParser.parseSearch(output.get());
        });
    }

    private static byte[] toFloat32Blob(List<Double> vec) {
        if (vec.size() != DIM) {
            throw new IllegalArgumentException("Embedding dimension mismatch. Expected " + DIM + ", got " + vec.size());
        }
        ByteBuffer bb = ByteBuffer.allocate(DIM * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (double d : vec) bb.putFloat((float) d);
        return bb.array();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
