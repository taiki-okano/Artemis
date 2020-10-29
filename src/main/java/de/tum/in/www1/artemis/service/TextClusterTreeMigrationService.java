package de.tum.in.www1.artemis.service;

import static java.util.stream.Collectors.toList;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import de.tum.in.www1.artemis.domain.text.*;
import de.tum.in.www1.artemis.repository.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TextClusterTreeMigrationService {

    private final Logger log = LoggerFactory.getLogger(TextClusterTreeMigrationService.class);

    private final TextPairwiseDistanceRepository textPairwiseDistanceRepository;

    private final TextTreeNodeRepository textTreeNodeRepository;

    private final TextClusterRepository textClusterRepository;

    private final TextExerciseRepository textExerciseRepository;

    private final TextBlockRepository textBlockRepository;


    public TextClusterTreeMigrationService(TextPairwiseDistanceRepository textPairwiseDistanceRepository, TextClusterRepository textClusterRepository, TextExerciseRepository textExerciseRepository, TextTreeNodeRepository textTreeNodeRepository, TextBlockRepository textBlockRepository) {
        this.textClusterRepository = textClusterRepository;
        this.textPairwiseDistanceRepository = textPairwiseDistanceRepository;
        this.textExerciseRepository = textExerciseRepository;
        this.textTreeNodeRepository = textTreeNodeRepository;
        this.textBlockRepository = textBlockRepository;
    }

    @NotNull
    private List<TextExercise> findAllTextExercises() {
        return textExerciseRepository.findAll();
    }

    @NotNull
    private List<TextExercise> findAllTextExercisesNeedingMigration() {
        return findAllTextExercises().parallelStream().filter(
            ex -> ex.getDueDate() != null && ex.getDueDate().isBefore(ZonedDateTime.now()) && (ex.getPairwiseDistances() == null || ex.getPairwiseDistances().isEmpty())
        ).collect(toList());
    }

    @NotNull
    private List<TextCluster> findTextClustersForExercise(TextExercise exercise) {
        return textClusterRepository.findAllByExercise(exercise);
    }

    @NotNull
    private List<TextBlock> findTextBlocksForExercise(TextExercise exercise) {
        return textBlockRepository.findAllBySubmission_Participation_Exercise_Id(exercise.getId());
    }

    /**
     * Migrate Feedback on Text Exercises to use TextBlocks as reference.
     */
    @PostConstruct()
    public void migrate() {
        log.info("Starting Migration of Text Feedback");
        final long start = System.currentTimeMillis();

        final List<TextExercise> exerciseList = findAllTextExercisesNeedingMigration();
        log.info("Found {} Text Exercises in need of a migration.", exerciseList.size());

        int clusterCounter = 0;
        int blockCounter = 0;

        for (TextExercise exercise : exerciseList) {
            List<TextCluster> clusters = findTextClustersForExercise(exercise);
            List<TextBlock> blocks = findTextBlocksForExercise(exercise);
            List<TextPairwiseDistance> pairwiseDistances = new ArrayList<>();
            List<TextTreeNode> clusterTree = new ArrayList<>();

            Map<String, TextBlock> blockMap = new HashMap<>();
            int treeIdCounter = 0;
            for (TextBlock block : blocks) {
                // Update treeIds of blocks
                block.setTreeId(treeIdCounter++);
                blockMap.put(block.getId(), block);
            }
            treeIdCounter++;
            for (TextCluster cluster : clusters) {
                // Update treeIds of clusters and update block references
                cluster.setTreeId(treeIdCounter++);
                List<TextBlock> clusterBlocks = cluster.getBlocks();
                cluster.setBlocks(clusterBlocks.stream().map(b -> blockMap.get(b.getId())).collect(toList()));

                // Update pairwise distances from the distanceMatrix
                double[][] distanceMatrix = cluster.getDistanceMatrix();
                for (int i = 0; i < distanceMatrix.length; i++) {
                    for (int j = i + 1; j < distanceMatrix.length; j++) {
                        TextPairwiseDistance dist = new TextPairwiseDistance();
                        dist.setBlockI(cluster.getBlocks().get(i).getTreeId());
                        dist.setBlockJ(cluster.getBlocks().get(j).getTreeId());
                        dist.setDistance(distanceMatrix[i][j]);
                        dist.setExercise(exercise);
                        pairwiseDistances.add(dist);
                    }
                }

                // Add tree nodes for blocks and clusters
                TextTreeNode clusterNode = new TextTreeNode().exercise(exercise);
                clusterNode.setParent(-1);
                clusterNode.setChildSize(cluster.getBlocks().size());
                clusterNode.setChild(cluster.getTreeId());
                clusterNode.setLambdaVal(Double.POSITIVE_INFINITY);
                clusterTree.add(clusterNode);

                for (TextBlock blockInCluster : cluster.getBlocks()) {
                    TextTreeNode blockNode = new TextTreeNode().exercise(exercise);
                    blockNode.setChild(blockInCluster.getTreeId());
                    blockNode.setLambdaVal(Double.POSITIVE_INFINITY);
                    blockNode.setChildSize(1);
                    blockNode.setParent(cluster.getTreeId());
                    clusterTree.add(blockNode);
                }

                // Add artificial unconnected root node
                TextTreeNode rootNode = new TextTreeNode().exercise(exercise);
                rootNode.setChild(cluster.getBlocks().size());
                rootNode.setLambdaVal(Double.POSITIVE_INFINITY);
                rootNode.setChildSize(cluster.getBlocks().size());
                rootNode.setParent(-1);
                clusterTree.add(rootNode);
            }

            // Save all to the database
            textBlockRepository.saveAll(blocks);
            textClusterRepository.saveAll(clusters);
            textPairwiseDistanceRepository.saveAll(pairwiseDistances);
            textTreeNodeRepository.saveAll(clusterTree);
        }

        log.info("Finished migrating {} Text Exercises in {}ms.", exerciseList.size(), (System.currentTimeMillis() - start));
        log.info("Processed {} Text Clusters and {} Text Blocks.", clusterCounter, blockCounter);
    }
}
