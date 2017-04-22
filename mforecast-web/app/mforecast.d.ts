declare module 'mforecast' {
    export namespace HelloWorld {
        export function sayHello(): string;
    }
    export namespace Mfc {
        export function createRequest(): string;

        export function parseCashflow(str: string): CashflowSpec;

        export function parseResponse(resp: string): Allocation[];

        export function validateLocalDate(str: string): boolean;

        export function validatePeriod(str: string): boolean;

        export function rollout(c: CashflowSpec, start: string, period: string): Cashflow[];

        export function makeRequest(cfs: Cashflow[]): string;

        export function genChartInput(as: Allocation[], cfs: Cashflow[]): any[][];

        export function genAmChartInput(as: Allocation[], cfs: Cashflow[]): any[];
    }
    export interface Allocation {
        allocated: Date;
        expiry: Date;
        account: Account;
        category: Category;
        amount: Number;
    }
    export interface Category {
        name: string;
    }
    export interface Account {
        name: string;
    }
    export interface CashflowSpec {
        verb: string;
        amount: number;
        catOrAcc: string;
        periodValue: number;
        periodUnit: PeriodUnit;
    }
    export interface PeriodUnit {
        short: string;
        singular: string;
        plural: string;
    }
    export interface Cashflow {}
}
