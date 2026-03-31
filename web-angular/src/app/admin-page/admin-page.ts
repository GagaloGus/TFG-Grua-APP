import { Component } from '@angular/core';
import { RouterOutlet, RouterLinkWithHref } from '@angular/router';
import { AuthService } from '../services/auth-service/auth-service';

@Component({
  selector: 'app-admin-page',
  imports: [RouterOutlet, RouterLinkWithHref],
  templateUrl: './admin-page.html',
  styleUrl: './admin-page.scss',
})
export class AdminPage {


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
  
    getEmail(): string {
      return this.authService.getEmail()
    }
}
