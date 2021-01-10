import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { SERVER_API_URL } from 'app/app.constants';
import { StudentLeaderboardElement } from 'app/shared/dashboards/student-leaderboard/student-leaderboard.model';

export type EntityArrayResponseType = HttpResponse<StudentLeaderboardElement[]>;

@Injectable({ providedIn: 'root' })
export class StudentLeaderboardService {
    private resourceUrl = SERVER_API_URL + 'api';

    constructor(private http: HttpClient) {}

    /**
     * Finds StudentLeaderboard data.
     * @param id - id of diagram to be found.
     * @param courseId - id of the course.
     */
    getLeaderboard(courseId: number, mode: number): Observable<EntityArrayResponseType> {
        return this.http.get<StudentLeaderboardElement[]>(`${this.resourceUrl}/student-leaderboard/course/${courseId}/mode/${mode}`, { observe: 'response' });
    }
}
