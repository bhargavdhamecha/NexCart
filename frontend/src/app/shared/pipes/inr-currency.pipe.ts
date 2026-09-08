import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'inrCurrency', standalone: true })
export class InrCurrencyPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '₹0';
    return '₹' + value.toLocaleString('en-IN');
  }
}
