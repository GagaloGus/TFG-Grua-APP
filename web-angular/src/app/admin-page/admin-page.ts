import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLinkWithHref, Router } from '@angular/router';
import { AuthService } from '../services/auth-service/auth-service';
import { DataService } from '@services/data.service';

@Component({
  selector: 'app-admin-page',
  imports: [RouterOutlet, RouterLinkWithHref],
  templateUrl: './admin-page.html',
  styleUrl: './admin-page.scss',
})
export class AdminPage {
  errorMsg = signal('');
  finishedLoading = signal(false);

  constructor(private authService: AuthService, private router: Router, private dataService: DataService) { }

  async cargarServicios() {
    try {
      this.dataService.cargarServicios();
    } catch (err: any) {
      throw Error(`Error al cargar servicios: ${err.message}`);
    }
  }

  async cargarVehiculos() {
    try {
      this.dataService.cargarVehiculos()
    } catch (err: any) {
      throw Error(`Error al cargar vehiculos: ${err.message}`);
    }
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn()
  }

  isAdmin(): boolean {
    return this.authService.isAdmin()
  }

  logout() {
    console.log("bai")
    this.authService.logout()
    this.router.navigate(['/home'])
  }

  getEmail() {
    return this.authService.getEmail()
  }
}
