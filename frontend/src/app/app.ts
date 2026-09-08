import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ToastComponent } from './shared/components/toast/toast.component';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ToastComponent],
  template: `
    <app-navbar />
    <main>
      <router-outlet />
    </main>
    <app-footer />
    <app-toast />
  `,
  styles: [`
    main {
      min-height: calc(100vh - 120px);
    }
  `],
})
export class App implements OnInit {
  private themeService = inject(ThemeService);

  ngOnInit(): void {
    // ThemeService constructor sets the initial data-theme attribute.
    // Access it here to ensure it's eagerly initialized.
    void this.themeService.theme();
  }
}
