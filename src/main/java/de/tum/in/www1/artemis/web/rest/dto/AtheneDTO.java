package de.tum.in.www1.artemis.web.rest.dto;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.tum.in.www1.artemis.domain.text.TextBlockType;
import de.tum.in.www1.artemis.domain.text.TextCluster;
import de.tum.in.www1.artemis.domain.text.TextTreeNode;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AtheneDTO {

    private List<TextBlockDTO> blocks = new ArrayList<>();

    private Map<Integer, TextCluster> clusters = new LinkedHashMap<>();

    private List<TextTreeNode> clusterTree = new ArrayList<>();

    private List<List<Double>> distanceMatrix = new ArrayList<>();

    public List<TextBlockDTO> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<TextBlockDTO> blocks) {
        this.blocks = blocks;
    }

    public Map<Integer, TextCluster> getClusters() {
        return clusters;
    }

    public void setClusters(Map<Integer, TextCluster> clusters) {
        this.clusters = clusters;
    }

    public List<TextTreeNode> getClusterTree() { return clusterTree; }

    public void setClusterTree(List<TextTreeNode> clusterTree) { this.clusterTree = clusterTree; }

    public List<List<Double>> getDistanceMatrix() { return distanceMatrix; }

    public void setDistanceMatrix(List<List<Double>> distanceMatrix) { this.distanceMatrix = distanceMatrix; }

    // Inner DTO
    public static class TextBlockDTO {

        private String id;

        private long submissionId;

        private String text;

        private int startIndex;

        private int endIndex;

        private Integer treeId;

        private TextBlockType type = TextBlockType.AUTOMATIC;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getSubmissionId() {
            return submissionId;
        }

        public void setSubmissionId(long submissionId) {
            this.submissionId = submissionId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
        }

        public TextBlockType getType() {
            return type;
        }

        public void setType(TextBlockType type) {
            this.type = type;
        }

        public Integer getTreeId() { return treeId; }

        public void setTreeId(Integer treeId) { this.treeId = treeId; }
    }

}
