import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth-service/auth-service';
import { Usuario } from '@services/tablas.supabase';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.css',
})


export class Login {
  email = "";
  password = ""
  error = signal('');
  validatingLogin = signal(false);
  data = signal<Usuario[]>([]);

  constructor(
    private authService: AuthService,
    private router: Router) { }

  async login() {
    this.validatingLogin.set(true)
    let sucess = await this.authService.login(this.email, this.password)

    if (sucess == '') {
      console.log("Valido")
      this.router.navigate(['/home'])
    }
    else {
      console.log(sucess)
      this.error.set(sucess)
    }
    this.validatingLogin.set(false)
  }
}
