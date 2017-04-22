import {Component} from '@angular/core';
import {Allocation, Cashflow, CashflowSpec, Mfc} from 'mforecast';
import {MfcReqService} from './mfc-req.service';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';
import 'rxjs/add/operator/concatMap';
import 'rxjs/add/operator/publishReplay';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/let';
import 'rxjs/add/operator/withLatestFrom';
import * as c3 from 'c3';

declare var google: any;
@Component({
    selector: 'my-app',
    template: `<h1>Money Forecast</h1>

    <div>
      <div id="c3chart"></div>
      <cashflow-input-list
          (cashflows)="onCfChange($event)"
          (start)="onStartChange($event)"
          (forecastPeriod)="onPeriodChange($event)"></cashflow-input-list>
      <button type="button" (click)="planClick.next(0)">Plan</button>
    </div>
    `,
})
export class AppComponent {

    private cashflowsSpecSubj = new ReplaySubject<CashflowSpec[]>(1);
    private startSubj: ReplaySubject<string> = new ReplaySubject<string>(1);
    private periodSubj: ReplaySubject<string> = new ReplaySubject<string>(1);
    private planClick = new ReplaySubject<any>(1);

    private plan0: Observable<([Cashflow[], Allocation[]])> =
        this.planClick
            .do(x => console.log(x))
            .withLatestFrom(this.cashflowsSpecSubj, (x, c) => c)
            .do(x => console.log(x))
            .withLatestFrom(this.startSubj, (a, b) => [a, b])
            .do(x => console.log(x))
            .withLatestFrom(this.periodSubj, (a, b) => [a[0], a[1], b])
            .do(x => console.log(x))
            .concatMap((tup: any) => this.makeReq(tup[0], tup[1], tup[2]))
            .map(tup => this.processResp(tup[0], tup[1]))
            .publishReplay(1)
            .refCount();

    private plan: Observable<Allocation[]> = this.plan0
                                                 .map(tup => tup[1]);


    private chart: any;
    private c3ChartData = this.plan0.map(tup => Mfc.genC3ChartInput(tup[1], tup[0]));

    constructor(private mfcSvc: MfcReqService) {
        this.c3ChartData
            .subscribe(cData => this.drawC3Chart(cData));
    }

    makeReq(cfspecs: CashflowSpec[], start: string, period: string): Observable<[Cashflow[], string]> {
        console.log(`pars: ${start} ${period}`);
        let cfs0: Cashflow[][] = cfspecs.map(c => Mfc.rollout(c, start, period));
        let cfs: Cashflow[] = [].concat.apply([], cfs0);
        let req = Mfc.makeRequest(cfs);
        console.log(`Sending ${new Date()}`);
        return this.mfcSvc.plan(req).map(r => [cfs, r]);
    }

    private processResp(cfs: Cashflow[], resp: string): [Cashflow[], Allocation[]] {
        console.log(`Received  ${new Date()}`);
        let res: [Cashflow[], Allocation[]] = [cfs, Mfc.parseResponse(resp)];
        console.log(`Parsed  ${new Date()}`);
        return res;
    }

    public onCfChange(cfs: CashflowSpec[]): void {
        console.log(cfs);
        this.cashflowsSpecSubj.next(cfs);
    }

    public onStartChange(s: string): void {
        this.startSubj.next(s);
    }

    public onPeriodChange(s: string): void {
        this.periodSubj.next(s);
    }

    public drawC3Chart(cdata: any[]): void {
        console.log(cdata);
        let config: any = {
            bindto: '#c3chart',
            color: {
                pattern: [
                    // https://github.com/internalfx/distinct-colors
                    // http://tools.medialab.sciences-po.fr/iwanthue/
                    // // Generate colors (as Chroma.js objects)
                    // var colors = paletteGenerator.generate(
                    //     20, // Colors
                    //     function(color){ // This function filters valid colors
                    //         var hcl = color.hcl();
                    //         return hcl[0]>=0 && hcl[0]<=300
                    //             && hcl[1]>=35 && hcl[1]<=100
                    //             && hcl[2]>=55 && hcl[2]<=80;
                    //     },
                    //     true, // Using Force Vector instead of k-Means
                    //     50, // Steps (quality)
                    //     false, // Ultra precision
                    //     'Default' // Color distance type (colorblindness)
                    // );
                    // // Sort colors by differenciation first
                    // colors = paletteGenerator.diffSort(colors, 'Default');
                    "#f2bf33",
                    "#6c76f9",
                    "#71de52",
                    "#e94a5e",
                    "#00be6f",
                    "#f381a6",
                    "#69de71",
                    "#2dbaff",
                    "#c3cf2a",
                    "#c96945",
                    "#71db9a",
                    "#ff9258",
                    "#3b962f",
                    "#ffa37e",
                    "#6c9500",
                    "#eaae82",
                    "#c4cd72",
                    "#a67c45",
                    "#acc281",
                    "#918a4b"
                ]
            },
            size: {
                height: 600
            },
            data: {
                columns: cdata[1],
                type: 'bar',
                groups: [cdata[1].map((x: any) => x[0])]
            },
            bar: {
                width: {
                    ratio: 0.8 // this makes bar width 50% of length between ticks
                }
            },
            axis: {
                x: {
                    type: 'categories',
                    categories: cdata[0],
                    tick: {
                        outer: false
                    }
                },
            },
            subchart: {
                show: true,
                axis: {
                    x: {
                        show: false
                    }
                }
            }
        };
        this.chart = c3.generate(config);
        console.log(this.chart);
        console.log(JSON.stringify(config));
        this.chart.zoom([0, Math.min(cdata[0].length, 24)]);
    }

}
