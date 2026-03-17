import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SupabaseService } from '../supabase.service';
import { AuthService } from '../services/auth-service/auth-service';
import { debug } from 'console';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.css',
})


export class Login {
  username = "";
  password = ""
  error = false
  validatingLogin = false
  data: any[] = [];
  constructor(
    private authService: AuthService,
    private router: Router,
    private supabaseService: SupabaseService) { }

  async login() {
    this.validatingLogin = true
    let sucess = await this.authService.login(this.username, this.password)
    this.validatingLogin = false

    if (sucess) {
      console.log("Valido")
      this.router.navigate(['/home'])
    }
    else {
      console.log("Error!")
      this.error = true
    }
  }
}
