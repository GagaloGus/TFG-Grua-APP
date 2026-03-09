import { Component } from '@angular/core';
import { AuthService } from '../services/auth';
import { Router } from '@angular/router';
import { FormsModule } from "@angular/forms";

@Component({
  selector: 'app-createuser',
  imports: [FormsModule],
  templateUrl: './createuser.html',
  styleUrl: './createuser.css',
})
export class Createuser {
  username = ""
  password = ""
  error = false

  constructor(private authService: AuthService, private router: Router) { }

  createUser() {
    const sucess = this.authService.createUser(this.username, this.password)

    if (sucess)
      this.router.navigate(['/home'])
    else
      this.error = true
  }
}
