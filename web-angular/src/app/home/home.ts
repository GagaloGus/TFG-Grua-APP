import { Component } from '@angular/core';
import { RouterLink } from "@angular/router";
import { AuthService } from '../services/auth-service/auth-service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {

  constructor(private authService:AuthService){}

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn()
  }

  isAdmin(): boolean {
    return this.authService.isAdmin()
  }

  logout() {
    this.authService.logout()
  }

  getEmail() {
    return this.authService.getEmail()
  }
}
