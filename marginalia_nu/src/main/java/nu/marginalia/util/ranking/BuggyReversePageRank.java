package nu.marginalia.util.ranking;


public class BuggyReversePageRank extends RankingAlgorithm {


    public BuggyReversePageRank(RankingDomainFetcher domains, String... origins) {
        super(domains, origins);
    }

    @Override
    RankVector createNewRankVector(RankVector rank) {

        double rankNorm = rank.norm();
        RankVector newRank = new RankVector(0);

        for (int domainId = 0; domainId < domainIndexToId.size(); domainId++) {

            var links = linkDataSrc2Dest[domainId];

            if (links != null && links.size() > 0) {
                double newRankValue = 0;

                for (int j = 0; j < links.size(); j++) {
                    newRankValue += rank.get(links.getQuick(j)) / links.size();
                }

                newRank.set(domainId, 0.85*newRankValue/rankNorm);
            }
        }
        return newRank;
    }

    @Override
    void adjustRankVector(RankVector vector, double dNorm, double oldNorm) {
        originDomainIds.forEach(id -> vector.increment(domainIdToIndex.get(id), dNorm/oldNorm));
    }

}
