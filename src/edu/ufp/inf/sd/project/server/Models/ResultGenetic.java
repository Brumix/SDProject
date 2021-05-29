package edu.ufp.inf.sd.project.server.Models;

import edu.ufp.inf.sd.project.util.geneticalgorithm.CrossoverStrategies;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ResultGenetic {

    private final String id;
    private int trie = 0;
    private int result;
    private int strategie = 1;
    private int changestrategie = 0;
    private LocalDateTime lastUpdate;
    // 1= working  0= closed
    private int isFinish = 1;
    private Map<Integer, CrossoverStrategies> strategiesMap = new HashMap<>();


    public ResultGenetic(String id, int result) {
        this.id = id;
        this.result = result;
        this.lastUpdate = LocalDateTime.now();
        strategiesMap.put(1, CrossoverStrategies.ONE);
        strategiesMap.put(2, CrossoverStrategies.TWO);
        strategiesMap.put(3, CrossoverStrategies.THREE);
    }

    public String getId() {
        return id;
    }

    public int getTrie() {
        return trie;
    }


    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public void atulizaResult(int result) {
        this.trie++;
        this.setResult(result);
        this.lastUpdate = LocalDateTime.now();
    }

    public CrossoverStrategies nextStrategy() {
        this.strategie = (this.strategie % 3) + 1;
        this.changestrategie++;
        return strategiesMap.get(this.strategie);
    }

    public void close() {
        this.isFinish = 0;
    }

    public int getStatus() {
        return this.isFinish;
    }

    public int getChangestrategie() {
        return this.changestrategie;
    }

    public String isqueueStuck() {
        if ((LocalDateTime.now().getSecond() - this.lastUpdate.getSecond()) >= 5)
            return this.id;
        return null;
    }

}
