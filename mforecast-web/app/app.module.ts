import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppComponent} from './app.component';
import {HttpModule} from '@angular/http';
import {MfcReqService} from './mfc-req.service';
import {CashflowInputComponent} from './cashflow-input.component';
import {FormsModule} from '@angular/forms';
import {CashflowInputListComponent} from './cashflow-input-list.component';
import {AmChartsModule} from 'amcharts3-angular2';

@NgModule({
    imports: [
        BrowserModule,
        HttpModule,
        FormsModule,
        AmChartsModule
    ],
    declarations: [
        AppComponent,
        CashflowInputComponent,
        CashflowInputListComponent
    ],
    providers: [
        MfcReqService
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
