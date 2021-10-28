import { Feedback } from 'app/entities/feedback.model';
import { StructuredGradingCriterionServiceInterface } from 'app/exercises/shared/structured-grading-criterion/structured-grading-criterion.service';

export class MockStructuredGradingCriterionService implements StructuredGradingCriterionServiceInterface {
    computeTotalScore(assessments: Feedback[]): number {
        return 0;
    }

    updateFeedbackWithStructuredGradingInstructionEvent(feedback: Feedback, event: any): void {}

    calculateScoreForGradingInstructions(feedback: Feedback, score: number, gradingInstructions: any): number {
        return 0;
    }
}
