import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Home } from './home/home';
import { AdminPage } from './admin-page/admin-page';
import { AdminDashboard } from './admin-page/admin-dashboard/admin-dashboard';
import { AdminServicios } from './admin-page/admin-servicios/admin-servicios';
import { AdminCreateuser } from './admin-page/admin-createuser/admin-createuser';
import { AdminUsers } from './admin-page/admin-users/admin-users';
import { AdminVehiculos } from './admin-page/admin-vehiculos/admin-vehiculos';
import { authGuard } from '@services/auth-guard/auth-guard';

export const routes: Routes = [

    {path: "admin", component:AdminPage, canActivate: [authGuard],
        children:[
            {path: "dashboard", component:AdminDashboard},
            {path: "servicios", component:AdminServicios},
            {path: "createuser", component:AdminCreateuser},
            {path: "usuarios", component:AdminUsers},
            {path: "vehiculos", component:AdminVehiculos},
            {path: '', redirectTo: 'dashboard', pathMatch: 'full'},
        ]
    },
    {path: "login", component:Login},
    {path: "home", component:Home},

    // Ruta vacia
    {path: '', redirectTo: '/home', pathMatch: 'full'},

    // Ruta incorrecta
    {path: '**', redirectTo: '/home', pathMatch: 'full'},
];
