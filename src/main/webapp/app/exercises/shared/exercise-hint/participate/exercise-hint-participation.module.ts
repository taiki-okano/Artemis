import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { ExerciseHintStudentComponent, ExerciseHintStudentDialogComponent } from 'app/exercises/shared/exercise-hint/participate/exercise-hint-student-dialog.component';
import { ArtemisMarkdownModule } from 'app/shared/markdown.module';

@NgModule({
    imports: [SharedModule, ArtemisMarkdownModule],
    declarations: [ExerciseHintStudentDialogComponent, ExerciseHintStudentComponent],
    entryComponents: [ExerciseHintStudentDialogComponent],
    exports: [ExerciseHintStudentDialogComponent, ExerciseHintStudentComponent],
})
export class ArtemisExerciseHintParticipationModule {}
