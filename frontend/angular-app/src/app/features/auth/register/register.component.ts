import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

function validateCpf(cpf: string): boolean {
  const digits = cpf.replace(/\D/g, '');
  if (digits.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(digits)) return false;
  const calc = (d: string, len: number): number => {
    let sum = 0;
    for (let i = 0; i < len; i++) sum += parseInt(d[i]) * (len + 1 - i);
    const r = (sum * 10) % 11;
    return r === 10 ? 0 : r;
  };
  return calc(digits, 9) === parseInt(digits[9]) && calc(digits, 10) === parseInt(digits[10]);
}

function formatCpf(value: string): string {
  const d = value.replace(/\D/g, '').slice(0, 11);
  return d.replace(/(\d{3})(\d)/, '$1.$2')
          .replace(/(\d{3})(\d)/, '$1.$2')
          .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

function formatCep(value: string): string {
  const d = value.replace(/\D/g, '').slice(0, 8);
  return d.replace(/(\d{5})(\d{1,3})$/, '$1-$2');
}

export const UF_LIST = [
  'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
  'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'
];

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly ufs = UF_LIST;

  // Dados pessoais
  firstName = '';
  lastName  = '';
  email     = '';
  cpf       = '';
  password  = '';
  confirmPassword = '';

  // Endereço (opcional)
  cep         = '';
  logradouro  = '';
  numero      = '';
  complemento = '';
  bairro      = '';
  cidade      = '';
  estado      = '';

  loading       = signal(false);
  error         = signal('');
  cpfError      = signal('');
  passwordError = signal('');

  onCpfInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cpf = formatCpf(input.value);
    input.value = this.cpf;
    this.cpfError.set(this.cpf.length === 14 && !validateCpf(this.cpf) ? 'CPF inválido' : '');
  }

  onCepInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cep = formatCep(input.value);
    input.value = this.cep;
  }

  onPasswordChange(): void {
    if (this.confirmPassword && this.password !== this.confirmPassword) {
      this.passwordError.set('As senhas não coincidem');
    } else {
      this.passwordError.set('');
    }
  }

  private hasAddress(): boolean {
    return !!(this.logradouro || this.numero || this.bairro || this.cidade || this.estado || this.cep);
  }

  onSubmit(): void {
    this.error.set('');

    if (!this.firstName || !this.lastName || !this.email || !this.cpf || !this.password || !this.confirmPassword) {
      this.error.set('Preencha todos os campos obrigatórios.');
      return;
    }
    if (!validateCpf(this.cpf)) { this.cpfError.set('CPF inválido'); return; }
    if (this.password !== this.confirmPassword) { this.passwordError.set('As senhas não coincidem'); return; }
    if (this.password.length < 8) { this.error.set('A senha deve ter ao menos 8 caracteres.'); return; }

    this.loading.set(true);

    const body: Record<string, unknown> = {
      firstName: this.firstName,
      lastName:  this.lastName,
      email:     this.email,
      cpf:       this.cpf,
      password:  this.password,
      address:   this.hasAddress() ? {
        logradouro:  this.logradouro  || null,
        numero:      this.numero      || null,
        complemento: this.complemento || null,
        bairro:      this.bairro      || null,
        cidade:      this.cidade      || null,
        estado:      this.estado      || null,
        cep:         this.cep         || null,
      } : null,
    };

    this.http.post(`${environment.userServiceUrl}/users/register`, body).subscribe({
      next: () => {
        this.auth.login(this.email, this.password).subscribe({
          next: () => this.router.navigate(['/products']),
          error: () => this.router.navigate(['/login']),
        });
      },
      error: (err) => {
        this.error.set(err.error?.detail ?? 'Erro ao realizar cadastro. Tente novamente.');
        this.loading.set(false);
      },
    });
  }
}
