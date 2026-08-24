import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UploadResponse {
  buildingsCreated: number;
}

export interface LocateRequest {
  x: number;
  y: number;
  z: number;
}

export interface LocateResponse {
  building: string | null;
  floor: string | null;
}

export interface StrategyResponse {
  strategy: 'JAVA' | 'POSTGIS';
}

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(private http: HttpClient) {}

  uploadBuildings(file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadResponse>('/api/buildings/upload', formData);
  }

  locate(request: LocateRequest): Observable<LocateResponse> {
    return this.http.post<LocateResponse>('/api/locate', request);
  }

  getStrategy(): Observable<StrategyResponse> {
    return this.http.get<StrategyResponse>('/api/strategy');
  }

  setStrategy(strategy: 'JAVA' | 'POSTGIS'): Observable<StrategyResponse> {
    return this.http.put<StrategyResponse>('/api/strategy', { strategy });
  }
}
