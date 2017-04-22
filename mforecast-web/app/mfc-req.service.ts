import {Http, Headers} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/throw';
import 'rxjs/add/operator/map';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/catch';
import {Injectable} from '@angular/core';

@Injectable()
export class MfcReqService {
    private static readonly _headers = {headers: MfcReqService.genHeaders()};

    private static genHeaders(): Headers {
        let h = new Headers();
        h.append('Content-Type', 'application/json');
        return h;
    }


    constructor(protected http: Http) {
    }


    plan(req: string): Observable<string> {
        return this.http.post(`/api/plan`, req, MfcReqService._headers)
                   .map(res => JSON.stringify(res.json()))
                   .catch(err => {
                       console.error(`Something went wrong`);
                       console.error(err);
                       return Observable.of('');
                   });
    }


}