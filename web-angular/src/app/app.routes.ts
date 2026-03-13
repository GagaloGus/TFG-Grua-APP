import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Home } from './home/home';
import { AdminPage } from './admin-page/admin-page';
import { AdminDashboard } from './admin-dashboard/admin-dashboard';
import { AdminServicios } from './admin-servicios/admin-servicios';

export const routes: Routes = [
    
    {
        path: "admin", component:AdminPage,
        children:[
            {path: "admin-dashboard", component:AdminDashboard},
            {path: "admin-servicios", component:AdminServicios},
        ]
    },
    {path: "login", component:Login},
    {path: "home", component:Home},
    {path: '', redirectTo: '/login', pathMatch: 'full'},
];
