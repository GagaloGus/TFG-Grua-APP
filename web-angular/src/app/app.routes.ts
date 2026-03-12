import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Home } from './home/home';

export const routes: Routes = [
    {path: "login", component:Login},
    {path: "home", component:Home},
    {path: '', redirectTo: '/login', pathMatch: 'full'},
];
