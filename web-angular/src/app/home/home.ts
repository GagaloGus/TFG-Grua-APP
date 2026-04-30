import { Component, OnInit } from '@angular/core';
import { RouterLink } from "@angular/router";
import { AuthService } from '../services/auth-service/auth-service';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {

  constructor(private authService: AuthService, private title: Title) { }

  ngOnInit(): void {
    this.title.setTitle("TowApp")
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn()
  }

  isAdmin(): boolean {
    return this.authService.isAdmin()
  }

  logout() {
    this.authService.logout()
  }

  get rol(){
    return this.authService.rol
  }

  get email() {
    return this.authService.email
  }

  get nombre() {
    return this.authService.nombre
  }

  get apellido1() {
    return this.authService.apellido1
  }

  get avatar() {
    return this.authService.avatarUrl()
  }
}
