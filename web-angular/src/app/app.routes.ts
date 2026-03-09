import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Home } from './home/home';
import { Admin } from './admin/admin';
import { authGuard } from './guards/auth-guard';
import { Createuser } from './createuser/createuser';
import { About } from './about/about';

export const routes: Routes = [
    {path: 'home', component: Home},
    {path: 'about', component: About},
    {path: 'login', component: Login},
    {path: 'createUser', component: Createuser},
    // CanActivate aqui significa que si se recarga la pagina, te devuelve a /home
    {path: 'admin', component: Admin, canActivate: [authGuard]},
    {path: '', redirectTo: '/home', pathMatch: 'full'},
];
