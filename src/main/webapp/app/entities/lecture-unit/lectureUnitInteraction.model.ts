import { LectureUnit } from 'app/entities/lecture-unit/lectureUnit.model';
import { User } from 'app/core/user/user.model';

export class LectureUnitInteraction {
    public id?: number;
    public progressInPercent?: number;
    public student?: User;
    public lectureUnit?: LectureUnit;
}
