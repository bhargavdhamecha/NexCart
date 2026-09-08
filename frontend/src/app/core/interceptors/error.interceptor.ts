import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err) => {
      const message =
        err?.error?.message || err?.message || 'Something went wrong. Please try again.';
      // Skip 401 as JWT interceptor handles it
      if (err.status !== 401) {
        toast.error(message);
      }
      return throwError(() => err);
    })
  );
};
