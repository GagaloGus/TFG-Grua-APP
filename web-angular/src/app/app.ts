import { Component, signal , inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet, RouterLinkWithHref, RouterModule } from "@angular/router";
import { AuthService } from './services/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLinkWithHref, RouterModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('AppSeguridad');
    authService = inject(AuthService)
  router = inject(Router)
  
  logout(){
    this.authService.logout()
    this.router.navigate(["/home"])
  }

  isLoggedIn():boolean{
    return this.authService.isLoggedIn()
  }

  isAdmin():boolean{
    return this.authService.isAdmin()
  }
  
  getUsername():string{
    return this.authService.getUsername()
  }
}
