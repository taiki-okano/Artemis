package de.tum.in.www1.artemis.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import de.tum.in.www1.artemis.domain.Feedback;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.util.TextExerciseUtilService;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.enumeration.Language;
import de.tum.in.www1.artemis.domain.text.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtheneClusterTreeTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    // Sentences taken from the book Object-Oriented Software Engineering by B. Bruegge and A. Dutoit
    private static final String[] BLOCK_TEXT = { "The purpose of science is to describe and understand complex systems,",
            "such as a system of atoms, a society of human beings, or a solar system.",
            "Traditionally, a distinction is made between natural sciences and social sciences to distinguish between two major types of systems.",
            "The purpose of natural sciences is to understand nature and its subsystems.",
            "Natural sciences include, for example, biology, chemistry, physics, and paleontology.",
            "The purpose of the social sciences is to understand human beings.",
            "Social sciences include psychology and sociology.",
            "There is another type of system that we call an artificial system.",
            "Examples of artificial systems include the space shuttle, airline reservation systems, and stock trading systems.",
            "Herbert Simon coined the term sciences of the artificial to describe the sciences that deal with artificial systems [Simon, 1970].",
            "Whereas natural and social sciences have been around for centuries, the sciences of the artificial are recent." };

    @Autowired
    FeedbackService feedbackService;

    @Autowired
    AutomaticTextFeedbackService automaticTextFeedbackService;

    @Autowired
    FeedbackRepository feedbackRepository;

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    TextExerciseRepository textExerciseRepository;

    @Autowired
    TextBlockRepository textBlockRepository;

    @Autowired
    TextClusterRepository textClusterRepository;

    @Autowired
    TextTreeNodeRepository textTreeNodeRepository;

    @Autowired
    TextPairwiseDistanceRepository textPairwiseDistanceRepository;

    @Autowired
    TextSubmissionRepository textSubmissionRepository;

    @Autowired
    ExerciseService exerciseService;

    @Autowired
    RequestUtilService request;

    @Autowired
    DatabaseUtilService database;

    private TextExerciseUtilService textExerciseUtilService = new TextExerciseUtilService();

    private List<TextExercise> exercises;

    private List<TextSubmission> submissions = new ArrayList<>();

    private List<TextBlock> blocks = new ArrayList<>();

    private List<TextCluster> clusters = new ArrayList<>();

    private List<TextTreeNode> treeNodes;

    private List<TextPairwiseDistance> pairwiseDistances;

    private TextSubmission submission;

    @BeforeAll
    public void init() {
        SecurityUtils.setAuthorizationObject(); // TODO: Why do we need this
        database.addUsers(10, 1, 1);
        database.addCourseWithOneFinishedTextExercise();
        database.addCourseWithOneFinishedTextExercise();

        exercises = textExerciseRepository.findAll();

        // Initialize data for the main exercise
        TextExercise exercise = exercises.get(0);

        initializeBlocksAndSubmissions(exercise);
        initializeClusters(exercise);

        // Read pre-computed results from JSON
        try {
            treeNodes = textExerciseUtilService.parseClusterTree(exercise);
            pairwiseDistances = textExerciseUtilService.parsePairwiseDistances(exercise);
        }
        catch (ParseException | IOException e) {
            database.resetDatabase();
            fail("JSON files for clusterTree or pairwiseDistances not successfully read/parsed.");
        }

        textTreeNodeRepository.saveAll(treeNodes);
        textPairwiseDistanceRepository.saveAll(pairwiseDistances);

        // Initialize data for the second exercise
        TextExercise exercise2 = exercises.get(1);
        submission = ModelFactory.generateTextSubmission("Submission to be deleted...", Language.ENGLISH, true);
        database.saveTextSubmission(exercise2, submission, "student1");

        TextBlock block = ModelFactory.generateTextBlock(0, 1, "b1");
        block.setSubmission(submission);
        block.setTreeId(11);
        textBlockRepository.save(block);

        TextCluster cluster = new TextCluster().exercise(exercise2);
        textClusterRepository.save(cluster);

        TextTreeNode incorrectTreeNode = new TextTreeNode().exercise(exercise2);
        incorrectTreeNode.setChild(-1);
        textTreeNodeRepository.save(incorrectTreeNode);

        TextPairwiseDistance incorrectPairwiseDistance = new TextPairwiseDistance().exercise(exercise2);
        textPairwiseDistanceRepository.save(incorrectPairwiseDistance);
    }

    @AfterAll
    public void tearDown() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testUniqueProperties() {
        TextExercise exercise = exercises.get(0);

        // BlockI < BlockJ and distance >= 0 should hold
        for (TextPairwiseDistance dist : pairwiseDistances) {
            assertThat(dist.getBlockI(), lessThan(dist.getBlockJ()));
            assertThat(dist.getDistance(), greaterThanOrEqualTo(0.));
        }
        // Only half of the matrix is stored in the database, as it is symmetrical (Main diagonal also not stored).
        int matrixSize = (blocks.size() - 1) * blocks.size() / 2; // Gives sum of numbers from 1 to (blocks.size() - 1)
        assertThat(pairwiseDistances, hasSize(matrixSize));

        // Getter and setter for lambda value tested
        TextTreeNode testNode = new TextTreeNode();
        testNode.setLambdaVal(Double.POSITIVE_INFINITY);
        assertThat(ReflectionTestUtils.getField(testNode, "lambdaVal"), equalTo(-1.));
        assertThat(testNode.getLambdaVal(), equalTo(Double.POSITIVE_INFINITY));
        // isBlockNode() tested
        testNode.setChildSize(1);
        assertThat(testNode.isBlockNode(), equalTo(true));
        testNode.setChildSize(2);
        assertThat(testNode.isBlockNode(), equalTo(false));

        // The following should hold for the root node:
        TextTreeNode rootNode = textTreeNodeRepository.findAllByParentAndExercise(-1L, exercise).get(0);
        assertThat((int) rootNode.getChild(), equalTo(blocks.size()));
        assertThat(rootNode.getLambdaVal(), equalTo(Double.POSITIVE_INFINITY));
        assertThat(rootNode.isBlockNode(), equalTo(false));
        assertThat((int) rootNode.getChildSize(), equalTo(blocks.size()));

        // TreeIds of clusters not null
        assertThat(clusters.stream().map(TextCluster::getTreeId).collect(Collectors.toList()), everyItem(notNullValue()));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testTraversalForFeedback() {
        TextExercise exercise = exercises.get(0);
        List<TextTreeNode> clusterTree = textTreeNodeRepository.findAllByExercise(exercise);
        clusterTree.sort(Comparator.comparingLong(TextTreeNode::getChild));

        // Give manual feedback to block 3
        Result result3 = blocks.get(3).getSubmission().getResult();
        Feedback manualFeedback3 = new Feedback().reference(blocks.get(3).getId()).credits(1.).result(result3).detailText("Feedback text 3").type(FeedbackType.MANUAL);
        feedbackRepository.save(manualFeedback3);
        assertThat(feedbackRepository.findAll(), hasSize(1));
        assertThat(feedbackRepository.findAll().get(0).getType(), equalTo(FeedbackType.MANUAL));

        // Block 4 should receive automatic feedback, as it shares the cluster with 3
        Result result4 = blocks.get(4).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result4);
        List<Feedback> feedback4 = result4.getFeedbacks();
        assertThat(feedback4, hasSize(1));
        assertThat(feedback4.get(0).getType(), equalTo(FeedbackType.AUTOMATIC));

        // Block 2 shouldn't receive automatic_merged feedback, as it is too far from cluster 13 in the tree
        Result result2 = blocks.get(2).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result2);
        List<Feedback> feedback2 = result2.getFeedbacks();
        assertThat(feedback2, hasSize(0));

        // Block 8 should receive automatic_merged feedback, as it gets it from cluster 13 after the traversal
        Result result8 = blocks.get(8).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result8);
        List<Feedback> feedback8 = result8.getFeedbacks();
        assertThat(feedback8, hasSize(1));
        assertThat(feedback8.get(0).getType(), equalTo(FeedbackType.AUTOMATIC_MERGED));

        // Block 0 and 1 are blocks of the same submission
        // Block 0 should receive automatic_merged feedback, as it gets it from cluster 13 after the traversal
        // Block 1 shouldn't receive automatic_merged feedback, as it is too far from cluster 13
        Result result0and1 = blocks.get(0).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result0and1);
        List<Feedback> feedback0and1 = result0and1.getFeedbacks();
        assertThat(feedback0and1, hasSize(1));
        assertThat(feedback0and1.get(0).getType(), equalTo(FeedbackType.AUTOMATIC_MERGED));

        // Block 7 shouldn't receive automatic_merged feedback, as it is too far from cluster 14
        Result result7 = blocks.get(7).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result7);
        List<Feedback> feedback7 = result7.getFeedbacks();
        assertThat(feedback7, hasSize(0));

        // Block 10 should receive automatic feedback, as it shares the clusters with block 8
        Result result10 = blocks.get(10).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result10);
        List<Feedback> feedback10 = result10.getFeedbacks();
        assertThat(feedback10, hasSize(1));
        assertThat(feedback10.get(0).getType(), equalTo(FeedbackType.AUTOMATIC_MERGED));

        // Block 5 shouldn't receive automatic_merged feedback, as it is too far from cluster 14
        Result result5 = blocks.get(5).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result5);
        List<Feedback> feedback5 = result5.getFeedbacks();
        assertThat(feedback5, hasSize(0));

        // Block 6 shouldn't receive automatic_merged feedback, as it is too far from cluster 14
        Result result6 = blocks.get(6).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result6);
        List<Feedback> feedback6 = result6.getFeedbacks();
        assertThat(feedback6, hasSize(0));

        // Block 9 should receive automatic_merged feedback, as it receives it from cluster 17 after the traversal
        Result result9 = blocks.get(9).getSubmission().getResult();
        automaticTextFeedbackService.suggestFeedback(result9);
        List<Feedback> feedback9 = result9.getFeedbacks();
        assertThat(feedback9, hasSize(1));
        assertThat(feedback9.get(0).getType(), equalTo(FeedbackType.AUTOMATIC_MERGED));

        // For this example execution, 4 additional feedback elements were created by traversing the tree
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testExerciseRemoval() throws Exception {
        TextExercise exercise = exercises.get(1);

        // Test cascading removals for exercise
        exerciseService.delete(exercise.getId(), true, true);
        assertThat(textExerciseRepository.findById(exercise.getId()).isPresent(), equalTo(false));
        assertThat(textBlockRepository.findAllBySubmission_Participation_Exercise_IdAndTreeIdNotNull(exercise.getId()), hasSize(0));
        assertThat(textSubmissionRepository.findById(submission.getId()), equalTo(Optional.empty()));
        assertThat(textClusterRepository.findAllByExercise(exercise), hasSize(0));
        assertThat(textTreeNodeRepository.findAllByExercise(exercise), hasSize(0));
        assertThat(textPairwiseDistanceRepository.findAllByExercise(exercise), hasSize(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCourseRemoval() throws Exception {
        // Test cascading removals for course
        database.addCourseWithOneFinishedTextExercise();
        List<TextExercise> exercises = textExerciseRepository.findAll();
        TextExercise exercise = exercises.get(exercises.size() - 1);

        TextTreeNode newNode = new TextTreeNode().exercise(exercise);
        newNode.setChild(111);
        TextPairwiseDistance newDist = new TextPairwiseDistance().exercise(exercise);
        textPairwiseDistanceRepository.save(newDist);
        textTreeNodeRepository.save(newNode);

        request.delete("/api/courses/" + exercise.getCourseViaExerciseGroupOrCourseMember().getId(), HttpStatus.OK);
        assertThat(textExerciseRepository.findById(exercise.getId()).isPresent(), equalTo(false));
        assertThat(textTreeNodeRepository.findAllByExercise(exercise), hasSize(0));
        assertThat(textPairwiseDistanceRepository.findAllByExercise(exercise), hasSize(0));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testExamRemoval() throws Exception {
        // Test cascading removals for exam
        database.addCourseExamExerciseGroupWithOneTextExercise();
        List<TextExercise> exercises = textExerciseRepository.findAll();
        TextExercise exercise = exercises.get(exercises.size() - 1);

        TextTreeNode newNode = new TextTreeNode().exercise(exercise);
        newNode.setChild(112);
        TextPairwiseDistance newDist = new TextPairwiseDistance().exercise(exercise);
        textPairwiseDistanceRepository.save(newDist);
        textTreeNodeRepository.save(newNode);

        request.delete("/api/courses/" + exercise.getCourseViaExerciseGroupOrCourseMember().getId() + "/exams/" + exercise.getExerciseGroup().getExam().getId(), HttpStatus.OK);
        assertThat(textExerciseRepository.findById(exercise.getId()).isPresent(), equalTo(false));
        assertThat(textTreeNodeRepository.findAllByExercise(exercise), hasSize(0));
        assertThat(textPairwiseDistanceRepository.findAllByExercise(exercise), hasSize(0));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testExerciseGroupRemoval() throws Exception {
        // Test cascading removals for exercise group
        database.addCourseExamExerciseGroupWithOneTextExercise();
        List<TextExercise> exercises = textExerciseRepository.findAll();
        TextExercise exercise = exercises.get(exercises.size() - 1);

        TextTreeNode newNode = new TextTreeNode().exercise(exercise);
        newNode.setChild(113);
        newNode.setChildSize(0);
        newNode.setParent(112);
        newNode.setLambdaVal(-1);
        TextPairwiseDistance newDist = new TextPairwiseDistance().exercise(exercise);
        textPairwiseDistanceRepository.save(newDist);
        textTreeNodeRepository.save(newNode);

        request.delete("/api/courses/" + exercise.getCourseViaExerciseGroupOrCourseMember().getId() + "/exams/" + exercise.getExerciseGroup().getExam().getId() + "/exerciseGroups/"
                + exercise.getExerciseGroup().getId(), HttpStatus.OK);
        assertThat(textExerciseRepository.findById(exercise.getId()).isPresent(), equalTo(false));
        assertThat(textTreeNodeRepository.findAllByExercise(exercise), hasSize(0));
        assertThat(textPairwiseDistanceRepository.findAllByExercise(exercise), hasSize(0));
    }

    /**
     * Initializes TextBlocks and TextSubmissions from the static blockText array for given exercise
     * @param exercise
     */
    private void initializeBlocksAndSubmissions(TextExercise exercise) {
        // Create text blocks and first submission, save submission
        submission = ModelFactory.generateTextSubmission(BLOCK_TEXT[0] + " " + BLOCK_TEXT[1], Language.ENGLISH, true);
        database.saveTextSubmissionWithResultAndAssessor(exercise, submission, "student1", "tutor1");

        TextBlock bl = new TextBlock().automatic().startIndex(0).endIndex(1).submission(submission).text(BLOCK_TEXT[0]).treeId(0);
        bl.computeId();
        blocks.add(bl);

        bl = new TextBlock().automatic().startIndex(1).endIndex(2).submission(submission).text(BLOCK_TEXT[1]).treeId(1);
        bl.computeId();
        blocks.add(bl);

        submissions.add(submission);
        textSubmissionRepository.save(submission);
        Result result = submission.getResult();
        result.setSubmission(submission);
        resultRepository.save(result);

        // Create text blocks and submissions, save submissions
        for (int i = 2; i <= 10; i++) {
            submission = ModelFactory.generateTextSubmission(BLOCK_TEXT[i], Language.ENGLISH, true);
            database.saveTextSubmissionWithResultAndAssessor(exercise, submission, "student" + i, "tutor1");
            bl = new TextBlock().automatic().startIndex(0).endIndex(1).submission(submission).text(BLOCK_TEXT[i]).treeId(i);
            bl.computeId();
            blocks.add(bl);
            submissions.add(submission);
            textSubmissionRepository.save(submission);
            result = submission.getResult();
            result.setSubmission(submission);
            resultRepository.save(result);
        }
        textBlockRepository.saveAll(blocks);
    }

    /**
     * Initializes TextClusters for given exercise
     * @param exercise
     */
    private void initializeClusters(TextExercise exercise) {
        TextCluster cluster1 = new TextCluster().exercise(exercise);
        cluster1.addBlocks(blocks.get(3));
        cluster1.addBlocks(blocks.get(4));
        cluster1.setTreeId(13);
        blocks.add(3, blocks.remove(3).cluster(cluster1));
        blocks.add(4, blocks.remove(4).cluster(cluster1));

        TextCluster cluster2 = new TextCluster().exercise(exercise);
        cluster2.addBlocks(blocks.get(1));
        cluster2.addBlocks(blocks.get(7));
        cluster2.setTreeId(17);
        blocks.add(1, blocks.remove(1).cluster(cluster2));
        blocks.add(7, blocks.remove(7).cluster(cluster2));

        TextCluster cluster3 = new TextCluster().exercise(exercise);
        cluster3.addBlocks(blocks.get(2));
        cluster3.addBlocks(blocks.get(5));
        cluster3.addBlocks(blocks.get(6));
        cluster3.setTreeId(16);
        blocks.add(2, blocks.remove(2).cluster(cluster3));
        blocks.add(5, blocks.remove(5).cluster(cluster3));
        blocks.add(6, blocks.remove(6).cluster(cluster3));

        TextCluster cluster4 = new TextCluster().exercise(exercise);
        cluster4.addBlocks(blocks.get(0));
        cluster4.addBlocks(blocks.get(8));
        cluster4.addBlocks(blocks.get(10));
        cluster4.setTreeId(14);
        blocks.add(0, blocks.remove(0).cluster(cluster4));
        blocks.add(8, blocks.remove(8).cluster(cluster4));
        blocks.add(10, blocks.remove(10).cluster(cluster4));

        clusters.add(cluster1);
        clusters.add(cluster2);
        clusters.add(cluster3);
        clusters.add(cluster4);
        textClusterRepository.saveAll(clusters);
        textBlockRepository.saveAll(blocks);
    }
}
