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
          <span>Plan date:</span>
          <input #tbStart type="text"
                 [ngModel]="start|async" size="9"
                 (keyup)="start.next(tbStart.value)">
        </div>
        <div class="input-box">
          <span>Relative start date: {{relativeStart | async}}</span>
        </div>

        <div class="input-box">
          <span>Forecast period:</span>
          <input #tbPeriod type="text"
                 [ngModel]="forecastPeriod|async" size="4"
                 (keyup)="forecastPeriod.next(tbPeriod.value)">
        </div>

        <div class="flex cashflow-view">
          <div class="cashflow-input">
          <textarea #taCashflow type="text" rows="15" cols="40"
                    [ngModel]="bulkTexts|async"
                    (keyup)="handleBulkChange(taCashflow.value)"></textarea>
          </div>
          <div class="cashflow-result divTable">
            <div class="divRow"
                 *ngFor="let c of (preCashflows|async)">

              <div *ngIf="!c.empty" class="hover">
                <i class="fa fa-search"></i>
                <div class="tooltip">
                  <div class="divTable">
                    <div class="divRow" *ngFor="let cc of rolloutOne(c.parsed)">
                      <div class="divCell">{{cc.date}}</div>
                      <div class="divCell">
                        <span *ngIf="cc.earn">+{{cc.amount}}</span>
                        <span *ngIf="!cc.earn">-{{cc.amount}}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div *ngIf="c.empty" class="divCell">&nbsp;</div>
              <div *ngIf="!c.empty && !c.valid" class="divCell invalid">Invalid</div>
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
              <div *ngIf="c.valid" class="divCell fill">
                <span *ngIf="c.hasPer">repeated every</span>
              </div>
              <div *ngIf="c.valid" class="divCell period">
                <span *ngIf="c.hasPer">
                  {{c.parsed.periodValue}} {{c.perPlural ? c.parsed.periodUnit.plural : c.parsed.periodUnit.singular}}</span>
                <span *ngIf="!c.hasPer">once</span>
              </div>
              <div *ngIf="c.valid" class="divCell fill">
                <span *ngIf="c.hasPer">for</span>
              </div>
              <div *ngIf="c.valid" class="divCell due">
                <span *ngIf="c.hasLen">
                  {{c.parsed.lenValue}} {{c.lenPlural ? c.parsed.lenUnit.plural : c.parsed.lenUnit.singular}}</span>
                <span *ngIf="!c.hasLen && c.hasPer">ethernity</span>
              </div>
            </div>
          </div>
        </div>

        <pre>    (s|e) &lt;amount&gt; on &lt;category|account&gt; [in &lt;period&gt;] [x &lt;period&gt; [for &lt;period&gt;]]
    period: &lt;N&gt; (d|w|m|y)</pre>


        <div>
          <div class="divTable">
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
        'e 100000 on Current x 1m',
        '',
        's 8000 on Living x 2w',
        's 28500 on Qtly-Bkv in 45d x 100d',
        's 30000 on Renovation in 4m x 1m for 6m'
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
    @Output() relativeStart = this.start.map(x => Mfc.nextMonthStart(x));


    forecastPeriod0 = new BehaviorSubject<string>('2y');
    @Output() forecastPeriod = this.forecastPeriod0.filter(x => Mfc.validatePeriod(x));

    rolled = Observable.combineLatest(this.cashflows, this.start, this.forecastPeriod,
        (sp, st, p) => ({specs: sp, start: st, per: p}))
                       .map(xs => this.rollout(xs.specs, xs.start, xs.per));

    private rolloutOne(spec: CashflowSpec): Cashflow[] {
        let res = this.rollout([spec], this.start0.getValue(), this.forecastPeriod0.getValue());
        console.log(res);
        return res;
    }

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
        let hasPer = valid && !(parsed.periodValue == null);
        let hasLen = valid && !(parsed.lenValue == null);
        return {
            str: x,
            empty: x === '',
            parsed: parsed,
            valid: valid,
            earn: valid && parsed.verb === 'earn',
            hasPer: hasPer,
            perPlural: valid && parsed.periodValue !== 1,
            hasDue: hasDue,
            duePlural: hasDue && parsed.dueValue !== 1,
            hasLen: hasLen,
            lenPlural: hasLen && parsed.lenValue !== 1
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

    showClick(c: CashflowSpec): void {
        console.log(c);
    }

}
