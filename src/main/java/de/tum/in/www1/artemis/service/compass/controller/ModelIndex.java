package de.tum.in.www1.artemis.service.compass.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import de.tum.in.www1.artemis.service.compass.umlmodel.UMLDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;
import de.tum.in.www1.artemis.service.compass.utils.CompassConfiguration;

public class ModelIndex {

    private final Logger log = LoggerFactory.getLogger(ModelIndex.class);

    private Queue<UMLElement> uniqueModelElementList;

    /**
     * Note: The key is the model submission id
     */
    private IMap<Long, UMLDiagram> modelMap;

    private IMap<UMLElement, Integer> elementSimilarityMap;

    public ModelIndex(Long exerciseId, HazelcastInstance hazelcastInstance) {
        log.info("Get similarities map from hazelcast for {} => ModelIndex()", exerciseId);
        elementSimilarityMap = hazelcastInstance.getMap("similarities - " + exerciseId);
        log.info("Get unique model element list from hazelcast for {} => ModelIndex()", exerciseId);
        uniqueModelElementList = hazelcastInstance.getQueue("elements - " + exerciseId);
        log.info("Get models map from hazelcast for {} => ModelIndex()", exerciseId);
        modelMap = hazelcastInstance.getMap("models - " + exerciseId);

    }

    /**
     * Get the internal similarity ID for the given model element. If the element is similar to an existing one, they share the same similarity id, i.e. they are in the same
     * similarity set. Otherwise, the given element does not belong to an existing similarity set and a new similarity ID is created for the element.
     *
     * @param element a model element for which the corresponding similarity ID should be retrieved
     * @return the similarity ID for the given model element, i.e. the ID of the similarity set the element belongs to
     */
    int retrieveSimilarityId(UMLElement element) {
        log.info("Check if {} is in element similarity map => retrieveSimilarityId()", element);
        if (elementSimilarityMap.containsKey(element)) {
            log.info("Return {} since it is already in element map => retrieveSimilarityId()", element);
            return elementSimilarityMap.get(element);
        }

        // Pair of similarity value and similarity ID
        var bestSimilarityFit = Pair.of(-1.0, -1);

        log.info("For each element in unique model element list make similarity comparison => retrieveSimilarityId()");
        for (final var knownElement : uniqueModelElementList) {
            final var similarity = knownElement.similarity(element);
            if (similarity > CompassConfiguration.EQUALITY_THRESHOLD && similarity > bestSimilarityFit.getFirst()) {
                // element is similar to existing element and has a higher similarity than another element
                bestSimilarityFit = Pair.of(similarity, knownElement.getSimilarityID());
            }
        }

        if (bestSimilarityFit.getFirst() != -1.0) {
            int similarityId = bestSimilarityFit.getSecond();
            element.setSimilarityID(similarityId);
            log.info("Put {} with {} into similarity map since a similar element has been found => retrieveSimilarityId()", element, similarityId);
            elementSimilarityMap.put(element, similarityId);
            return bestSimilarityFit.getSecond();
        }

        // element does not fit already known element / similarity set
        int similarityId = uniqueModelElementList.size();
        element.setSimilarityID(similarityId);
        log.info("Add element {} into unique model element list => retrieveSimilarityId()", element);
        uniqueModelElementList.add(element);
        log.info("Put element {} with similarityId {} into similarity map since no similar element has been found => retrieveSimilarityId", element, similarityId);
        elementSimilarityMap.put(element, similarityId);
        return similarityId;
    }

    /**
     * Add a new model to the model map.
     *
     * @param model the new model that should be added
     */
    public void addModel(UMLDiagram model) {
        log.info("Put submission {} into model map => addModel()", model.getModelSubmissionId());
        modelMap.put(model.getModelSubmissionId(), model);
    }

    /**
     * Get the model that belongs to the given submission ID from the model map.
     *
     * @param modelSubmissionId the ID of the submission to which the requested model belongs to
     * @return the model that belong to the submission with the given ID
     */
    public UMLDiagram getModel(long modelSubmissionId) {
        log.info("Get model {} => getModel()", modelSubmissionId);
        return modelMap.get(modelSubmissionId);
    }

    /**
     * Get the model map. It maps submission IDs to the models of the corresponding submissions.
     *
     * @return the model map
     */
    public Map<Long, UMLDiagram> getModelMap() {
        log.info("Get model map => getModelMap()");
        return modelMap;
    }

    /**
     * Get the collection of all the models.
     *
     * @return the collection of models
     */
    public Collection<UMLDiagram> getModelCollection() {
        log.info("Get current models => getModelCollection()");
        return modelMap.values();
    }

    /**
     * Get the number of models.
     *
     * @return the number of models
     */
    int getModelCollectionSize() {
        log.info("Get number of models => getModelCollectionSize()");
        return modelMap.size();
    }

    /**
     * Used for evaluation
     *
     * @return the number of unique model elements
     */
    public int getNumberOfUniqueElements() {
        log.info("Get number of unique model elements => getNumberOfUniqueElements()");
        return uniqueModelElementList.size();
    }

    /**
     * Used for evaluation
     *
     * @return the model element to similarity id mapping
     */
    public Map<UMLElement, Integer> getElementSimilarityMap() {
        log.info("Get similarities between elements => getElementSimilarityMap()");
        return elementSimilarityMap;
    }

    /**
     * Get the collection of unique elements. Each unique element represents a similarity set.
     *
     * @return the collection of unique elements
     */
    public Collection<UMLElement> getUniqueElements() {
        log.info("Get unique elements => getUniqueElements()");
        return uniqueModelElementList;
    }
}
