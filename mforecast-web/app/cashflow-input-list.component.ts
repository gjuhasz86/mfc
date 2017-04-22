import {Component, Output} from '@angular/core';
import {CashflowSpec, Mfc} from 'mforecast';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/scan';
import 'rxjs/add/observable/combineLatest';
import {Observable} from 'rxjs/Observable';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

@Component({
    selector: 'cashflow-input-list',
    template: `
      <div>
        <span>Start date:</span>
        <input #tbStart type="text"
               [ngModel]="start|async" size="20"
               (keyup)="start.next(tbStart.value)">

        <span>Forecast preriod:</span>
        <input #tbPeriod type="text"
               [ngModel]="forecastPeriod|async" size="20"
               (keyup)="forecastPeriod.next(tbPeriod.value)">

        <div *ngFor="let s of (texts|async); let i = index; trackBy: trackByFn">
          <input #tbCashflow type="text"
                 [ngModel]="s" size="60"
                 (keyup)="handleChange(i,tbCashflow.value)">
          <button type="button" (click)="handleAdd(i)">+</button>
          <button type="button" (click)="handleRemove(i)">x</button>
          <span>{{parse(s)}}</span>
        </div>
        <div>
          <textarea #taCashflow type="text" rows="15" cols="60"
                    [ngModel]="bulkTexts|async"
                    (keyup)="handleBulkChange(taCashflow.value)"></textarea>
        </div>
      </div>

    `
})
export class CashflowInputListComponent {

    private init = [
        's 60000 on Travel in 130d x 99y',
        's 30000 on Rent in 160d x 99y',
        's 120000 on Car in 190d x 99y',
        'e 100000 on Default in 5d x 99y',
        'e 200000 on Default in 35d x 99y',
        'e 100000 on Default in 65d x 99y',
        'e 100000 on Default in 95d x 99y',
        'e 100000 on Default in 125d x 99y',
        'e 100000 on Default in 155d x 99y',
        'e 100000 on Default in 185d x 99y'
    ];

    private changes = new BehaviorSubject<((ss: string[]) => string[])>(x => this.init);


    texts: Observable<string[]> = this.changes
                                      .scan(this.combineChanges, []);

    bulkTexts = this.texts.map(ts => ts.join('\n'));


    @Output() cashflows: Observable<CashflowSpec[]> = this.texts
                                                          .map(xs => xs.filter(x => x !== ''))
                                                          .map(xs => xs.map(x => Mfc.parseCashflow(x)))
                                                          .filter(xs => this.allValid(xs));

    start0 = new BehaviorSubject<string>(this.today());
    @Output() start = this.start0.filter(x => Mfc.validateLocalDate(x));


    forecastPeriod0 = new BehaviorSubject<string>('10y');
    @Output() forecastPeriod = this.forecastPeriod0.filter(x => Mfc.validatePeriod(x));

    private today(): string {
        let date = new Date();
        let dd: any = date.getDate();
        let mm: any = date.getMonth() + 1;

        let yyyy = date.getFullYear();
        if (mm < 10) {
            mm = '0' + mm;
        }
        if (dd < 10) {
            dd = '0' + dd;
        }
        return yyyy + '-' + mm + '-' + dd;
    }

    private allValid(objs: any[]): boolean {
        return objs.findIndex(x => (typeof x) === 'undefined') === -1;
    }

    public trackByFn(i: number, s: string) {
        return i;
    }

    parse(str: string): string {
        if (str === '') {
            return '';
        }
        let p = Mfc.parseCashflow(str);
        if (typeof p === 'undefined') {
            return `-invalid- [${str}]`;
        } else {
            return `${p.verb} ${p.amount} on ${p.catOrAcc} every ${p.periodValue} ${p.periodUnit.plural}`;
        }
    }

    combineChanges(acc: string[], f: ((ss: string[]) => string[])): string[] {
        return f(acc);
    }

    handleBulkChange(str: string): void {
        let lines = str.split(/\r?\n/);
        this.changes.next(x => lines);
    }

    handleChange(i: number, str: string): void {
        this.changes.next(x => {
            let copy = x.slice();
            copy[i] = str;
            return copy;
        });
    }

    handleAdd(i: number): void {
        this.changes.next(x => {
            let copy = x.slice();
            copy.splice(i + 1, 0, '');
            return copy;
        });
    }

    handleRemove(i: number): void {
        this.changes.next(x => {
            let copy = x.slice();
            copy.splice(i, 1);
            return copy;
        });
    }

}
