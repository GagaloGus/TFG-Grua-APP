import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth-service/auth-service';
import { Usuario } from '@services/tablas.supabase';
import { CommonModule } from '@angular/common';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule, RouterLink],
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})


export class Login implements OnInit{
  email = "";
  password = ""
  error = signal("");
  recordarSesion = false;
  validatingLogin = signal(false);
  data = signal<Usuario[]>([]);

  constructor(
    private authService: AuthService,
    private router: Router,
  private title: Title) { }

  ngOnInit(): void {
    this.title.setTitle("Login")
  }



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
