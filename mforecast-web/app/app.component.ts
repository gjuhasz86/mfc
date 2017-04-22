import {Component, OnInit} from '@angular/core';
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
import {AmChartsService} from 'amcharts3-angular2';

declare var google: any;
@Component({
    selector: 'my-app',
    template: `<h1>Money Forecast</h1>
    <cashflow-input-list
        (cashflows)="onCfChange($event)"
        (start)="onStartChange($event)"
        (forecastPeriod)="onPeriodChange($event)"></cashflow-input-list>
    <button type="button" (click)="planClick.next(0)">Plan</button>
    <div id="chartdiv" [style.width.%]="100" [style.height.px]="500"></div>
    <div id="chart_div"></div>
    <div *ngFor="let a of (plan | async)">{{a.allocated}} - {{a.expiry}} [{{a.category.name}}] {{a.amount}}</div>
    <pre>{{chartData | async | json}}</pre>
    `,
})
export class AppComponent implements OnInit {

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
    private chartData = this.plan0.map(tup => Mfc.genChartInput(tup[1], tup[0]));
    private amChartData = this.plan0.map(tup => Mfc.genAmChartInput(tup[1], tup[0]));

    constructor(private mfcSvc: MfcReqService,
                private AmCharts: AmChartsService) {
        // this.chartData
        //     .subscribe(cData => this.drawChart(cData));
        this.amChartData
            .subscribe(cData => this.drawAmChart(cData));
    }

    ngOnInit() {
        this.chart = this.AmCharts.makeChart('chartdiv', {
            "type": "serial",
            "categoryField": "category",
            "sequencedAnimation": false,
            "theme": "default",
            "categoryAxis": {
                "gridPosition": "start"
            },
            "chartScrollbar": {
                "enabled": true
            },
            "trendLines": [],
            "graphs": [],
            "guides": [],
            "valueAxes": [
                {
                    "id": "ValueAxis-1",
                    "stackType": "regular",
                    "title": "Axis title"
                }
            ],
            "allLabels": [
                {
                    "id": "Label-1"
                }
            ],
            "balloon": {},
            "legend": {
                "enabled": true,
                "useGraphSettings": true
            },
            "titles": [
                {
                    "id": "Title-1",
                    "size": 15,
                    "text": "Chart Title"
                }
            ],
            "dataProvider": []
        });
    }

    makeReq(cfspecs: CashflowSpec[], start: string, period: string): Observable<[Cashflow[], string]> {
        console.log(`pars: ${start} ${period}`);
        let cfs0: Cashflow[][] = cfspecs.map(c => Mfc.rollout(c, start, period));
        let cfs: Cashflow[] = [].concat.apply([], cfs0);
        let req = Mfc.makeRequest(cfs);
        console.log('Sending');
        console.log(req);
        return this.mfcSvc.plan(req).map(r => [cfs, r]);
    }

    private processResp(cfs: Cashflow[], resp: string): [Cashflow[], Allocation[]] {
        return [cfs, Mfc.parseResponse(resp)];
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

    public drawChart(cData: any[][]): void {
        // Create the data table.
        let data = google.visualization.arrayToDataTable(cData);

        // Set chart options
        let options = {
            height: 800,
            legend: {position: 'right', maxLines: 3},
            bar: {groupWidth: '75%'},
            isStacked: true,
            explorer: {
                axis: 'horizontal',
                keepInBounds: true,
                actions: ['dragToZoom', 'rightClickToReset'],
                maxZoomOut: 2,
                maxZoomIn: 4.0
            }
        };

        // Instantiate and draw our chart, passing in some options.
        let chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
        chart.draw(data, options);

    }

    public drawAmChart(cdata: any[]): void {
        console.log(JSON.stringify(cdata[1]));
        console.log(JSON.stringify(cdata[0].map((c: any) => ({
            "balloonText": "[[title]] of [[category]]:[[value]]",
            "fillAlphas": 1,
            "id": c,
            "title": c,
            "type": "column",
            "valueField": c
        }))));
        this.AmCharts.updateChart(this.chart, () => {
            // Change whatever properties you want, add event listeners, etc.
            this.chart.dataProvider = cdata[1];
            this.chart.graphs = cdata[0].map((c: any) => ({
                "balloonText": "[[title]] of [[category]]:[[value]]",
                "fillAlphas": 1,
                "id": c,
                "title": c,
                "type": "column",
                "valueField": c
            }));

        });

    }

}
