export interface FileMetadata {
    id: string;
    filename: string;
    originalName: string;
    contentType: string;
    size: number;
    uploadDate: string;
    ownerId: string;
    isPublic: boolean;
}

export interface FileUploadResponse {
    id: string;
    filename: string;
    originalName: string;
    contentType: string;
    size: number;
    uploadDate: string;
}

export interface FileUploadProgress {
    loaded: number;
    total: number;
    progress: number;
}

export interface FileListResponse {
    files: FileMetadata[];
    totalCount: number;
}

export interface FileSearchParams {
    page?: number;
    size?: number;
    sort?: string;
    search?: string;
} 