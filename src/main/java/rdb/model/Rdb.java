package rdb.model;

import java.util.List;

import redis.RedisRepository;

public record Rdb(
        List<AuxField> auxFields,
        List<RdbDbInfo> rdbDbInfos
) {
    public void init() {
        for (var dbInfo : rdbDbInfos) {
            dbInfo.rdbPairs().forEach(rdbPair -> RedisRepository.set(rdbPair.key(), rdbPair.value()));
            dbInfo.rdbExpirePairs().forEach(rdbExpirePair -> RedisRepository.setWithExpireTimestamp(rdbExpirePair.key(), rdbExpirePair.value(), rdbExpirePair.expireTime()));
        }
    }
}