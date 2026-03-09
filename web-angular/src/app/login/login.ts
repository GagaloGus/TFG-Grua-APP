import { Component } from '@angular/core';
import { AuthService } from '../services/auth';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = ""
  password = ""
  error = false

  constructor(private authService: AuthService, private router: Router){}

  login(){
    const sucess = this.authService.login(this.username, this.password)

    if(sucess)
      this.router.navigate(['/home'])
    else
      this.error = true
  }
}
