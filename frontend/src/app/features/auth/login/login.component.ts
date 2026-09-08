import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="container auth-shell">
      <div class="panel auth-card">
        <!-- Left brand side -->
        <div class="auth-side">
          <div>
            <div class="logo">
              <span class="logo-mark">N</span> NexCart
            </div>
            <h2>Welcome back.</h2>
            <p>Sign in to access your cart, checkout securely and track every order from one place.</p>
          </div>
          <div class="small" style="color:#d8ddff">Built for a production-style full-stack portfolio project.</div>
        </div>

        <!-- Right form side -->
        <form class="auth-form" [formGroup]="form" (ngSubmit)="onSubmit()" id="login-form">
          <h2>Sign in</h2>
          <p>Enter your email and password to continue.</p>

          <div class="field">
            <label for="email">Email</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              placeholder="you@example.com"
              [class.invalid]="isInvalid('email')"
            >
            @if (isInvalid('email')) {
              <div class="field-error">Please enter a valid email address.</div>
            }
          </div>

          <div class="field">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              placeholder="Your password"
              [class.invalid]="isInvalid('password')"
            >
            @if (isInvalid('password')) {
              <div class="field-error">Password must be at least 6 characters.</div>
            }
          </div>

          <button
            type="submit"
            class="primary-btn full"
            id="login-submit-btn"
            [disabled]="loading()"
          >
            {{ loading() ? 'Signing in...' : 'Sign in' }}
          </button>

          <div class="small center" style="margin-top:18px">
            New to NexCart?
            <a routerLink="/register" style="color:var(--brand);font-weight:800">Create account</a>
          </div>

          @if (error()) {
            <div class="form-error">{{ error() }}</div>
          }
        </form>
      </div>
    </div>
  `,
  styles: [`
    .auth-shell {
      min-height: 640px;
      display: grid;
      place-items: center;
      padding: 40px 0;
    }
    .auth-card {
      width: min(940px, 100%);
      display: grid;
      grid-template-columns: 1fr 1fr;
      overflow: hidden;
    }
    .auth-side {
      padding: 46px;
      background: linear-gradient(145deg, #1f48ff, #7247e7);
      color: white;
      min-height: 540px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 850;
      font-size: 20px;
      color: white;
    }
    .logo-mark {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: rgba(255,255,255,.25);
      display: grid;
      place-items: center;
      color: white;
      font-weight: 900;
    }
    .auth-side h2 { font-size: 38px; margin: 18px 0 10px; letter-spacing: -1px; }
    .auth-side p { color: #e7eaff; line-height: 1.6; }
    .auth-form { padding: 46px; background: var(--surface); }
    .auth-form h2 { margin: 0 0 8px; color: var(--text); }
    .auth-form p { margin: 0 0 24px; color: var(--muted); }
    .field { margin-bottom: 16px; }
    .field label {
      display: block;
      font-size: 13px;
      font-weight: 800;
      margin-bottom: 7px;
      color: var(--text);
    }
    .field input {
      width: 100%;
      height: 46px;
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 0 13px;
      outline: 0;
      background: var(--surface);
      color: var(--text);
      transition: .2s;
    }
    .field input:focus {
      border-color: #9aabff;
      box-shadow: 0 0 0 4px rgba(46,92,255,.1);
    }
    .field input.invalid { border-color: var(--danger); }
    .field-error { color: var(--danger); font-size: 12px; margin-top: 4px; }
    .form-error {
      background: #fff0f0;
      color: var(--danger);
      border-radius: 10px;
      padding: 12px;
      font-size: 13px;
      margin-top: 14px;
      text-align: center;
    }
    [data-theme="dark"] .form-error { background: rgba(239,68,68,.1); }
    .full { width: 100%; }
    .center { text-align: center; }
    @media(max-width:980px) {
      .auth-card { grid-template-columns: 1fr; }
      .auth-side { min-height: 260px; }
    }
    @media(max-width:680px) {
      .auth-form, .auth-side { padding: 28px; }
    }
  `],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  loading = signal(false);
  error = signal('');

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const { email, password } = this.form.value;
    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => {
        this.toastService.success('Signed in successfully');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Invalid email or password. Please try again.');
      },
    });
  }
}
