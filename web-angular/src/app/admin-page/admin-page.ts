import { Component } from '@angular/core';
import { RouterOutlet, RouterLinkWithHref, Router } from '@angular/router';
import { AuthService } from '../services/auth-service/auth-service';

@Component({
  selector: 'app-admin-page',
  imports: [RouterOutlet, RouterLinkWithHref],
  templateUrl: './admin-page.html',
  styleUrl: './admin-page.scss',
})
export class AdminPage {

    constructor(private authService:AuthService, private router: Router,){}

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

    get email() {
      return this.authService.email
    }

    get nombre(){
      return this.authService.nombre
    }
    
    get apellido1(){
      return this.authService.apellido1
    }

    get avatar(){
      return this.authService.avatarUrl()
    }
}
