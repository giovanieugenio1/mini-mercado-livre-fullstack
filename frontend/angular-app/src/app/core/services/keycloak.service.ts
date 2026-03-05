import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private readonly kc = new Keycloak({
    url: environment.keycloakUrl,
    realm: environment.keycloakRealm,
    clientId: environment.keycloakClientId,
  });

  async init(): Promise<boolean> {
    return this.kc.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      pkceMethod: 'S256',
    });
  }

  get isLoggedIn(): boolean {
    return this.kc.authenticated === true;
  }

  get username(): string {
    return (this.kc.tokenParsed as Record<string, unknown>)?.['preferred_username'] as string ?? '';
  }

  get userId(): string {
    return (this.kc.tokenParsed as Record<string, unknown>)?.['sub'] as string ?? '';
  }

  get roles(): string[] {
    return (this.kc.tokenParsed as Record<string, unknown>)?.['roles'] as string[] ?? [];
  }

  login(): void {
    this.kc.login({ redirectUri: window.location.href });
  }

  logout(): void {
    this.kc.logout({ redirectUri: window.location.origin });
  }

  async getToken(): Promise<string> {
    try {
      await this.kc.updateToken(30);
    } catch {
      this.kc.login({ redirectUri: window.location.href });
    }
    return this.kc.token ?? '';
  }
}
