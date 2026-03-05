import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

interface JwtPayload {
  sub: string;
  preferred_username: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  roles?: string[];
  exp: number;
}

const TOKEN_KEY = 'mm_access_token';
const REFRESH_KEY = 'mm_refresh_token';

const TOKEN_URL = `${environment.keycloakUrl}/realms/${environment.keycloakRealm}/protocol/openid-connect/token`;
const LOGOUT_URL = `${environment.keycloakUrl}/realms/${environment.keycloakRealm}/protocol/openid-connect/logout`;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _payload = signal<JwtPayload | null>(null);

  readonly isLoggedIn = computed(() => this._payload() !== null);
  readonly username = computed(() => this._payload()?.preferred_username ?? '');
  readonly userId = computed(() => this._payload()?.sub ?? '');
  readonly firstName = computed(() => this._payload()?.given_name ?? '');
  readonly roles = computed(() => this._payload()?.roles ?? []);

  constructor(private http: HttpClient, private router: Router) {
    this.restoreSession();
  }

  private restoreSession(): void {
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (!token) return;
    try {
      const payload = parseJwt(token);
      if (payload.exp * 1000 > Date.now()) {
        this._payload.set(payload);
      } else {
        this.clearStorage();
      }
    } catch {
      this.clearStorage();
    }
  }

  login(username: string, password: string): Observable<void> {
    const body = new URLSearchParams({
      grant_type: 'password',
      client_id: environment.keycloakClientId,
      username,
      password,
    });
    return this.http.post<TokenResponse>(TOKEN_URL, body.toString(), {
      headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
    }).pipe(
      tap(tokens => {
        sessionStorage.setItem(TOKEN_KEY, tokens.access_token);
        sessionStorage.setItem(REFRESH_KEY, tokens.refresh_token);
        this._payload.set(parseJwt(tokens.access_token));
      }),
      map(() => undefined),
    );
  }

  logout(): void {
    const refreshToken = sessionStorage.getItem(REFRESH_KEY);
    this.clearStorage();
    this._payload.set(null);
    if (refreshToken) {
      // Notifica Keycloak para invalidar a sessão (fire-and-forget)
      const body = new URLSearchParams({
        client_id: environment.keycloakClientId,
        refresh_token: refreshToken,
      });
      this.http.post(LOGOUT_URL, body.toString(), {
        headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
      }).subscribe({ error: () => {} });
    }
    this.router.navigate(['/products']);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  private clearStorage(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_KEY);
  }
}

function parseJwt(token: string): JwtPayload {
  const base64Url = token.split('.')[1];
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(atob(base64));
}
