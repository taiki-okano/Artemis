import { Component, Input, OnInit } from '@angular/core';
import { StudentLeaderboardElement } from 'app/shared/dashboards/student-leaderboard/student-leaderboard.model';
import { SortService } from 'app/shared/service/sort.service';
import { StudentLeaderboardService } from 'app/shared/dashboards/student-leaderboard/student-leaderboard.service';

@Component({
    selector: 'jhi-student-leaderboard',
    templateUrl: './student-leaderboard.component.html',
})
export class StudentLeaderboardComponent implements OnInit {
    @Input() courseId: number;

    leaderboardEntries: StudentLeaderboardElement[];
    show = false;
    sortPredicate = 'points';
    reverseOrder = false;

    constructor(private sortService: SortService, private studentLeaderboardService: StudentLeaderboardService) {}

    /**
     * Life cycle hook called by Angular to indicate that Angular is done creating the component
     */
    ngOnInit(): void {
        this.studentLeaderboardService.getLeaderboard(this.courseId, 0).subscribe((response) => {
            this.leaderboardEntries = response.body!;
            if (this.leaderboardEntries.length !== 0) {
                this.show = true;
            }
        });
    }

    sortRows() {
        this.sortService.sortByProperty(this.leaderboardEntries, this.sortPredicate, this.reverseOrder);
    }
}
