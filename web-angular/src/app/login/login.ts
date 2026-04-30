import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth-service/auth-service';
import { Usuario } from '@services/tablas.supabase';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule],
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})


export class Login {
  email = "";
  password = ""
  error = signal("");
  recordarSesion = false;
  validatingLogin = signal(false);
  data = signal<Usuario[]>([]);

  constructor(
    private authService: AuthService,
    private router: Router) { }

  async login() {
    this.error.set('')
    this.validatingLogin.set(true)
    const result = await this.authService.login(
      this.email,
      this.password,
      this.recordarSesion)

    if (result == '') {
      this.router.navigate(['/home'])
    }
    else {
      console.log(result)
      this.error.set(result)
    }
    this.validatingLogin.set(false)
  }

  mostrarPassword = signal(false)
}
