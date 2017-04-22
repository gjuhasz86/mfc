import {Component, Output} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';
import {CashflowSpec, Mfc} from 'mforecast';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/debounceTime';

@Component({
    selector: 'cashflow-input',
    template: `
      <div>
        <span>
          <span>{{(parsedCf | async)?.verb}}</span>
          <span>{{(parsedCf | async)?.amount}}</span> on
          <span>{{(parsedCf | async)?.category.name}}</span> every
          <span>{{(parsedCf | async)?.periodValue}}</span>
          <span>{{(parsedCf | async)?.periodUnit.plural}}</span>
        </span>
        <input #tbCashflow type="text" (keyup)="handleChange(tbCashflow.value)">
      </div>
    `,
})
export class CashflowInputComponent {
    strSubj = new ReplaySubject<string>(1);
    parsedCf: Observable<CashflowSpec> = this.strSubj
                                             .debounceTime(500)
                                             .map(s => this.parse(s));

    @Output() cashflow = this.parsedCf.filter(c => typeof c !== 'undefined');

    handleChange(str: string): void {
        console.log(str);
        this.strSubj.next(str);
    }

    parse(str: string): CashflowSpec {
        let tmp = Mfc.parseCashflow(str);
        return tmp;
    }
}
