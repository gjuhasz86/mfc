import {Component, Output} from '@angular/core';
import {Cashflow, CashflowSpec, Mfc} from 'mforecast';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/scan';
import 'rxjs/add/observable/combineLatest';
import {Observable} from 'rxjs/Observable';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

@Component({
    selector: 'cashflow-input-list',
    template: `
      <div>
        <div class="input-box">
          <span>Start date:</span>
          <input #tbStart type="text"
                 [ngModel]="start|async" size="20"
                 (keyup)="start.next(tbStart.value)">
        </div>

        <div class="input-box">
          <span>Forecast period:</span>
          <input #tbPeriod type="text"
                 [ngModel]="forecastPeriod|async" size="20"
                 (keyup)="forecastPeriod.next(tbPeriod.value)">
        </div>

        <div class="flex">
          <div class="cashflow-box">
          <textarea #taCashflow type="text" rows="15" cols="60"
                    [ngModel]="bulkTexts|async"
                    (keyup)="handleBulkChange(taCashflow.value)"></textarea>
          </div>
          <div class="cashflow-box divTable">
            <div class="divRow"
                 *ngFor="let c of (preCashflows|async)">
              <div *ngIf="c.empty">empty</div>
              <div *ngIf="!c.empty && !c.valid">invalid</div>
              <div *ngIf="c.valid" class="divCell verb">
                <span *ngIf="c.valid && c.earn" class="earn">Earn</span>
                <span *ngIf="c.valid && !c.earn" class="spend">Spend</span>
              </div>
              <div *ngIf="c.valid" class="divCell amount">
                <span *ngIf="c.valid && c.earn" class="earn">{{c.parsed.amount}}</span>
                <span *ngIf="c.valid && !c.earn" class="spend">{{c.parsed.amount}}</span>
              </div>
              <div *ngIf="c.valid" class="divCell fill">on</div>
              <div *ngIf="c.valid" class="divCell cat">{{c.parsed.catOrAcc}}</div>
              <div *ngIf="c.valid" class="divCell fill">
                <span *ngIf="c.hasDue">in</span></div>
              <div *ngIf="c.valid" class="divCell due">
                <span *ngIf="c.hasDue">
                  {{c.parsed.dueValue}} {{c.duePlural ? c.parsed.dueUnit.plural : c.parsed.dueUnit.singular}}</span>
                <span *ngIf="!c.hasDue">today</span>
              </div>
              <div *ngIf="c.valid" class="divCell fill">repeated every</div>
              <div *ngIf="c.valid" class="divCell period">
                {{c.parsed.periodValue}} {{c.perPlural ? c.parsed.periodUnit.plural : c.parsed.periodUnit.singular}}
              </div>
            </div>
          </div>
        </div>
        <div>
          <div class="cashflow-box divTable">
            <div class="divRow" *ngFor="let c of (rolled|async)">
              <div class="divCell">{{c.date}}</div>
              <div class="divCell">{{c.catOrAcc}}</div>
              <div class="divCell amount">
                <span *ngIf="c.earn" class="earn">{{c.amount}}</span>
                <span *ngIf="!c.earn" class="spend">{{c.amount}}</span>
              </div>
            </div>
          </div>
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

    preCashflows = this.texts.map(xs => xs.map(x => this.parse(x)));

    bulkTexts = this.texts.map(ts => ts.join('\n'));


    @Output() cashflows: Observable<CashflowSpec[]> = this.texts
                                                          .map(xs => xs.filter(x => x !== ''))
                                                          .map(xs => xs.map(x => Mfc.parseCashflow(x)))
                                                          .filter(xs => this.allValid(xs));


    start0 = new BehaviorSubject<string>(this.today());
    @Output() start = this.start0.filter(x => Mfc.validateLocalDate(x));


    forecastPeriod0 = new BehaviorSubject<string>('10y');
    @Output() forecastPeriod = this.forecastPeriod0.filter(x => Mfc.validatePeriod(x));

    rolled = Observable.combineLatest(this.cashflows, this.start, this.forecastPeriod,
        (sp, st, p) => ({specs: sp, start: st, per: p}))
                       .map(xs => this.rollout(xs.specs, xs.start, xs.per));

    private rollout(specs: CashflowSpec[], start: string, per: string): Cashflow[] {
        let cfs0: Cashflow[][] = specs.map(c => Mfc.rollout(c, start, per));
        let cfs: Cashflow[] = [].concat.apply([], cfs0);
        let res = cfs.map(c => this.mapCfToNative(c));
        return res.sort((a, b) => a.date < b.date ? -1 : a.date > b.date ? 1 : 0);
    }

    private mapCfToNative(c: any): Cashflow {
        let earn = !(c.account$1 == null);
        let res = {
            date: c.date$1.toString(),
            earn: earn,
            catOrAcc: earn ? c.account$1.name$1.toString() : c.category$1.name$1.toString(),
            amount: c.amount$1
        };

        return res;
    }

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

    parse(x: string): any {
        let parsed = Mfc.parseCashflow(x);
        let valid = !(parsed == null);
        let hasDue = valid && !(parsed.dueValue == null);
        return {
            str: x,
            empty: x === '',
            parsed: parsed,
            valid: valid,
            perPlural: valid && parsed.periodValue !== 1,
            earn: valid && parsed.verb === 'earn',
            hasDue: hasDue,
            duePlural: hasDue && parsed.dueValue !== 1,
        };
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
